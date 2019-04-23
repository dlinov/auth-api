package io.dlinov.auth.domain.algebras.services

import cats.data.EitherT
import cats.effect.{ContextShift, IO, Resource}
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.middleware.{FollowRedirect, Logger}
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.AppConfig.{AuthConfig, LogConfig}
import io.dlinov.auth.domain.{BaseService, ServiceError}

class CaptchaService(
    config: AuthConfig,
    loggerConfig: LogConfig,
    httpClient: Resource[IO, Client[IO]])(implicit contextShift: ContextShift[IO])
  extends BaseService {

  private val recaptchaSecret: String = config.recaptchaSecret

  private val baseRecaptchaUri = Uri
    .fromString(config.recaptchaUrl)
    .map(_.withQueryParam("secret", recaptchaSecret))

  private def wrapLogic[T](logic: Client[IO] ⇒ IO[T]): IO[T] = httpClient.use { client ⇒
    val followingRedirectsClient = FollowRedirect[IO](3)(client)
    val wrappedClient = if (loggerConfig.isEnabled) {
      Logger[IO](logHeaders = loggerConfig.logHeaders, logBody = loggerConfig.logBody) {
        followingRedirectsClient
      }
    } else {
      followingRedirectsClient
    }
    logic(wrappedClient)
  }

  def checkCaptcha[T](captcha: String): IO[ServiceResponse[Unit]] = {
    (for {
      uri ← EitherT(IO(baseRecaptchaUri.map(_.withQueryParam("response", captcha))))
        .leftMap(failure ⇒ ServiceError.invalidConfigError(s"Recaptcha url is invalid: ${failure.message}"))
      resp ← EitherT {
        wrapLogic(httpClient ⇒ {
          httpClient.get(uri)(r ⇒ IO {
            if (r.status.isSuccess) {
              Right(())
            } else {
              Left(ServiceError.invalidCaptchaError("Invalid captcha"))
            }
          })
        })
      }
    } yield resp).value
  }
}
