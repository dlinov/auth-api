package io.dlinov.auth.routes

import java.time.{ZoneOffset, ZonedDateTime}

import cats.Applicative
import cats.effect.Sync
import org.http4s.Uri
import org.http4s.rho.RhoRoutes
import io.dlinov.auth.BuildInfo
import HealthCheckRoutes.HealthInfo
import io.dlinov.auth.AuthenticationMiddlewareProvider
import io.dlinov.auth.routes.json.EntityEncoders
import org.http4s.rho.swagger.SwaggerSupport

class HealthCheckRoutes[F[_]](implicit override val syncF: Sync[F])
    extends SwaggerSupport[F]
    with Routes[F]
    with EntityEncoders[F] {
  implicit override protected def applicativeF: Applicative[F] = syncF

  private val swaggerLink =
    s"/swagger-ui/${BuildInfo.swaggerVersion}/index.html?url=%2Fswagger.json"
  private val anonSwaggerLink =
    s"/swagger-ui/${BuildInfo.swaggerVersion}/index.html?url=%2F${AuthenticationMiddlewareProvider.SwaggerAnonPath}"

  override val routes: RhoRoutes[F] = new RhoRoutes[F] {
    GET |>>
      TemporaryRedirect(Uri(path = swaggerLink))

    "Check app health status" **
      GET / "health" |>> {
      val now = ZonedDateTime.now.withZoneSameInstant(ZoneOffset.UTC)
      val message = HealthInfo(
        serverTime = now,
        build = BuildInfo.version,
        swagger = swaggerLink,
        anonymousSwagger = anonSwaggerLink
      )
      Ok(message)
    }
  }
}

object HealthCheckRoutes {
  case class HealthInfo(
      serverTime: ZonedDateTime,
      build: String,
      swagger: String,
      anonymousSwagger: String
  )
}
