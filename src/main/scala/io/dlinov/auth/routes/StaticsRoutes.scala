package io.dlinov.auth.routes

import cats.effect.{Blocker, ContextShift, Effect, Sync}
import io.dlinov.auth.dao.hikari.ec.BlockingECWrapper
import org.http4s.HttpRoutes
import org.http4s.server.staticcontent.webjarService
import org.http4s.server.staticcontent.WebjarService.{Config, WebjarAsset}

class StaticsRoutes[F[_]: Sync](ecWrapper: BlockingECWrapper)(
    implicit cs: ContextShift[F],
    effect: Effect[F]
) {

  protected val supportedExtensions: Set[String] =
    Set(".js", ".css", ".map", ".html", ".webm", ".png")

  val routes: HttpRoutes[F] = webjarService[F](
    Config(blocker = Blocker.liftExecutionContext(ecWrapper.ec), filter = isWebAsset)
  )

  protected def isWebAsset(asset: WebjarAsset): Boolean = {
    supportedExtensions.exists(asset.asset.endsWith)
  }
}
