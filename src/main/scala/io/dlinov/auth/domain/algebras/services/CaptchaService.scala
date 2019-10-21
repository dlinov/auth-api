package io.dlinov.auth.domain.algebras.services

import cats.data.EitherT
import cats.effect.{Concurrent, Resource}
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.middleware.{FollowRedirect, Logger}
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.AppConfig.{AuthConfig, LogConfig}
import io.dlinov.auth.domain.{BaseService, ServiceError}

class CaptchaService[F[_]](
    config: AuthConfig,
    loggerConfig: LogConfig,
    httpClient: Resource[F, Client[F]]
)(
    implicit
    concF: Concurrent[F]
) extends BaseService {

  private val recaptchaSecret: String = config.recaptchaSecret

  private val baseRecaptchaUri = Uri
    .fromString(config.recaptchaUrl)
    .map(_.withQueryParam("secret", recaptchaSecret))

  private def wrapLogic[T](logic: Client[F] ⇒ F[T]): F[T] = httpClient.use { client ⇒
    val followingRedirectsClient = FollowRedirect[F](3)(client)
    val wrappedClient = if (loggerConfig.isEnabled) {
      Logger[F](logHeaders = loggerConfig.logHeaders, logBody = loggerConfig.logBody) {
        followingRedirectsClient
      }
    } else {
      followingRedirectsClient
    }
    logic(wrappedClient)
  }

  def checkCaptcha[T](captcha: String): F[ServiceResponse[Unit]] = {
    (for {
      uri ← EitherT(concF.delay(baseRecaptchaUri.map(_.withQueryParam("response", captcha))))
        .leftMap(
          failure ⇒ ServiceError.invalidConfigError(s"Recaptcha url is invalid: ${failure.message}")
        )
      resp ← EitherT {
        wrapLogic(httpClient ⇒ {
          httpClient.get(uri)(
            r ⇒
              concF.delay {
                if (r.status.isSuccess) {
                  Right(())
                } else {
                  Left(ServiceError.invalidCaptchaError("Invalid captcha"))
                }
              }
          )
        })
      }
    } yield resp).value
  }
}
