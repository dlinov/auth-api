package io.dlinov.auth.routes.proxied

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import io.dlinov.auth.AppConfig.ProxyConfig
import org.http4s._
import org.http4s.client.Client
import org.http4s.headers._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AccountsProxySpec extends AnyWordSpecLike with Matchers {
  implicit private val asyncF: cats.effect.Async[IO] = IO.asyncForIO

  private val proxyConfig = ProxyConfig("downstream.internal", 9999)

  "AccountsProxy" should {
    "forward a GET under its root, sanitizing headers and overwriting X-UserName" in {
      var capturedRequest: Option[Request[IO]] = None
      val client                               = Client[IO] { req =>
        capturedRequest = Some(req)
        Resource.eval(IO(Response[IO](Status.Ok)))
      }
      val proxy    = new AccountsProxy[IO](client, proxyConfig)
      val user     = ProxyTestFixtures.userWithScopes("accounts_detail")
      val incoming = Request[IO](Method.GET, Uri.unsafeFromString("/api/accounts/123"))
        .withHeaders(
          Authorization(Credentials.Token(AuthScheme.Bearer, "client-supplied-secret")),
          Host("client-supplied-host"),
          Header.Raw(org.typelevel.ci.CIString("X-UserName"), "spoofed-user")
        )
      val req    = AuthedRequest(user, incoming)
      val result = proxy.routes.run(req).value.unsafeRunSync()
      result.map(_.status) mustBe Some(Status.Ok)

      val forwarded = capturedRequest.get
      forwarded.headers.get[Authorization] mustBe None
      forwarded.headers.get[Host] mustBe None
      forwarded.headers.get(org.typelevel.ci.CIString("X-UserName")).map(_.head.value) mustBe Some(
        user.userName
      )
      // destination host/port is always the rewritten (configured) one, never the client's
      forwarded.uri.authority.map(_.host.renderString) mustBe Some(proxyConfig.host)
      forwarded.uri.authority.flatMap(_.port) mustBe Some(proxyConfig.port)
    }

    "return Forbidden for a permission the caller does not have" in {
      val client = Client[IO](_ => Resource.eval(IO(Response[IO](Status.Ok))))
      val proxy  = new AccountsProxy[IO](client, proxyConfig)
      val user   = ProxyTestFixtures.userWithScopes("some_other_scope")
      val req    =
        AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString("/api/accounts/123")))
      val result = proxy.routes.run(req).value.unsafeRunSync()
      result.map(_.status) mustBe Some(Status.Forbidden)
    }

    "not match a sibling path sharing only a textual prefix" in {
      val client = Client[IO](_ => Resource.eval(IO(Response[IO](Status.Ok))))
      val proxy  = new AccountsProxy[IO](client, proxyConfig)
      val user   = ProxyTestFixtures.userWithScopes("accounts")
      val req    =
        AuthedRequest(user, Request[IO](Method.GET, Uri.unsafeFromString("/api/accountsXYZ")))
      proxy.routes.run(req).value.unsafeRunSync() mustBe None
    }
  }
}
