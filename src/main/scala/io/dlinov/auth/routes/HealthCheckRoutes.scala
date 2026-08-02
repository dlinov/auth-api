package io.dlinov.auth.routes

import java.time.{ZoneOffset, ZonedDateTime}

import cats.Applicative
import cats.effect.Async
import org.http4s.{HttpRoutes, Uri}
import org.http4s.headers.Location
import io.dlinov.auth.BuildInfo
import HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.json.EntityEncoders

class HealthCheckRoutes[F[_]](implicit override val asyncF: Async[F])
    extends Routes[F]
    with EntityEncoders[F] {
  implicit override protected def applicativeF: Applicative[F] = asyncF

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      TemporaryRedirect(Location(Uri.unsafeFromString("/health")))

    case GET -> Root / "health" =>
      val now     = ZonedDateTime.now.withZoneSameInstant(ZoneOffset.UTC)
      val message = HealthInfo(
        serverTime = now,
        build = BuildInfo.version
      )
      Ok(message)
  }
}

object HealthCheckRoutes {
  case class HealthInfo(
      serverTime: ZonedDateTime,
      build: String
  )
}
