package io.dlinov.auth.routes

import cats.effect.IO
import cats.syntax.either._
import io.circe.parser._
import org.http4s._

class HealthCheckRoutesSpec extends Http4sSpec {

  "Http4s BackOffice API" must {
    "respond to healthcheck request" in {
      val request = Request[IO](method = Method.GET, uri = Uri.uri("/health"))
      val resp    = services.run(request)
      check[String](
        resp,
        Status.Ok,
        bodyText ⇒ {
          val okOrError = for {
            parsedJson ← parse(bodyText).leftMap(_.getMessage())
            obj        ← parsedJson.asObject.toRight(s"$parsedJson is not a valid json object")
          } yield obj.keys.nonEmpty mustBe true
          okOrError.leftMap { e ⇒
            logger.warn(s"TEST FAILED, REASON: $e")
            fail(e)
          }.merge
        }
      )
    }
  }

}
