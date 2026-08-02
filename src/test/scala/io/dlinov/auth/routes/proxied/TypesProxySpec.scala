package io.dlinov.auth.routes.proxied

import cats.effect.{IO, Outcome, Resource}
import cats.effect.unsafe.implicits.global
import io.dlinov.auth.AppConfig.ProxyConfig
import org.http4s._
import org.http4s.client.Client
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class TypesProxySpec extends AnyWordSpecLike with Matchers {
  implicit private val asyncF: cats.effect.Async[IO] = IO.asyncForIO

  private val proxyConfig = ProxyConfig("downstream.internal", 9999)

  private def stubClient(response: IO[Response[IO]]): Client[IO] =
    Client[IO](_ => Resource.eval(response))

  "TypesProxy" should {
    "forward an authorized GET to each of its three roots" in {
      val client = stubClient(IO(Response[IO](Status.Ok).withEntity("downstream-ok")))
      val proxy  = new TypesProxy[IO](client, proxyConfig)
      val user   = ProxyTestFixtures.userWithScopes("types")

      List("/api/types", "/api/currencies", "/api/account_types").foreach { path =>
        val req    = AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString(path)))
        val result = proxy.routes.run(req).value.unsafeRunSync()
        result.map(_.status) mustBe Some(Status.Ok)
      }
    }

    "not match a non-GET method (no mutation is proxied for reference data)" in {
      val client = stubClient(IO(Response[IO](Status.Ok)))
      val proxy  = new TypesProxy[IO](client, proxyConfig)
      val user   = ProxyTestFixtures.userWithScopes("types")
      val req    = AuthedRequest(user, Request[IO](Method.POST, Uri.unsafeFromString("/api/types")))
      proxy.routes.run(req).value.unsafeRunSync() mustBe None
    }

    "return Forbidden when the caller lacks the types permission" in {
      val client = stubClient(IO(Response[IO](Status.Ok)))
      val proxy  = new TypesProxy[IO](client, proxyConfig)
      val user   = ProxyTestFixtures.userWithScopes("some_other_scope")
      val req    =
        AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString("/api/currencies")))
      val result = proxy.routes.run(req).value.unsafeRunSync()
      result.map(_.status) mustBe Some(Status.Forbidden)
    }

    "not recreate/re-acquire the downstream client per request" in {
      val callCount = new java.util.concurrent.atomic.AtomicInteger(0)
      val client    = Client[IO] { _ =>
        Resource.eval(IO {
          callCount.incrementAndGet()
          Response[IO](Status.Ok)
        })
      }
      val proxy = new TypesProxy[IO](client, proxyConfig)
      val user  = ProxyTestFixtures.userWithScopes("types")

      (1 to 5).foreach { _ =>
        val req = AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString("/api/types")))
        proxy.routes.run(req).value.unsafeRunSync()
      }
      // exactly one client.run invocation per request - never zero (would mean nothing was
      // proxied) and never more than one per call (which would indicate the client, or its
      // underlying connection resource, was being rebuilt/re-run redundantly per request).
      callCount.get() mustBe 5
    }

    "stream the response body correctly after the route has returned" in {
      val released  = new java.util.concurrent.atomic.AtomicBoolean(false)
      val bodyBytes = "streamed-response-body".getBytes("UTF-8")
      val client    = Client[IO] { _ =>
        Resource.make(
          IO(Response[IO](Status.Ok).withEntity(fs2.Stream.emits(bodyBytes).covary[IO]))
        )(_ => IO(released.set(true)))
      }
      val proxy  = new TypesProxy[IO](client, proxyConfig)
      val user   = ProxyTestFixtures.userWithScopes("types")
      val req    = AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString("/api/types")))
      val result = proxy.routes.run(req).value.unsafeRunSync()

      val response = result.get
      // The connection must not have been released yet: the caller (http4s server) hasn't
      // drained the body. Under the old `.use`-based implementation this would already be true
      // (a resource-safety bug) since `.use`'s effect completes as soon as the Response value is
      // produced, before any consumer reads the body.
      released.get() mustBe false

      val actualBytes = response.body.compile.toVector.unsafeRunSync().toArray
      new String(actualBytes, "UTF-8") mustBe "streamed-response-body"
      // Only now, after the body stream has been fully drained, is the connection released.
      released.get() mustBe true
    }

    "remain cancelable while the downstream connection is still being acquired, releasing " +
      "nothing since no resource was ever produced" in {
        val releaseCount = new java.util.concurrent.atomic.AtomicInteger(0)
        val client       = Client[IO] { _ =>
          // A `makeFull` acquire is interruptible via the threaded `poll`, modeling a real
          // client whose slow/hanging connect can be canceled/timed out mid-flight.
          Resource.makeFull[IO, Response[IO]] { poll =>
            poll(IO.sleep(5.seconds)) *> IO(Response[IO](Status.Ok))
          }(_ => IO(releaseCount.incrementAndGet()).void)
        }
        val proxy = new TypesProxy[IO](client, proxyConfig)
        val user  = ProxyTestFixtures.userWithScopes("types")
        val req   = AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString("/api/types")))

        val (outcome, elapsed) = (for {
          start   <- IO.monotonic
          fiber   <- proxy.routes.run(req).value.start
          _       <- IO.sleep(50.millis)
          _       <- fiber.cancel
          outcome <- fiber.join
          end     <- IO.monotonic
        } yield (outcome, end - start)).unsafeRunSync()

        outcome match {
          case Outcome.Canceled() => succeed
          case other              => fail(s"expected the fiber to be Canceled, was: $other")
        }
        // proves cancellation actually interrupted the acquire rather than being deferred until
        // the 5-second sleep completed on its own.
        elapsed must be < 2.seconds
        releaseCount.get() mustBe 0
      }

    "release the downstream connection exactly once when canceled mid-stream" in {
      val releaseCount = new java.util.concurrent.atomic.AtomicInteger(0)
      val client       = Client[IO] { _ =>
        Resource.make(
          IO(
            Response[IO](Status.Ok)
              .withEntity(fs2.Stream.awakeEvery[IO](50.millis).map(_ => 'x'.toByte))
          )
        )(_ => IO(releaseCount.incrementAndGet()).void)
      }
      val proxy = new TypesProxy[IO](client, proxyConfig)
      val user  = ProxyTestFixtures.userWithScopes("types")
      val req   = AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString("/api/types")))

      val outcome = (for {
        resultOpt <- proxy.routes.run(req).value
        response = resultOpt.get
        fiber   <- response.body.compile.drain.start
        _       <- IO.sleep(250.millis)
        _       <- fiber.cancel
        outcome <- fiber.join
      } yield outcome).unsafeRunSync()

      outcome match {
        case Outcome.Canceled() => succeed
        case other              => fail(s"expected the fiber to be Canceled, was: $other")
      }
      // exactly once: neither leaked (0) nor double-released (>1)
      releaseCount.get() mustBe 1
    }
  }
}
