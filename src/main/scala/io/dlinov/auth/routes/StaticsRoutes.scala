package io.dlinov.auth.routes

import cats.effect.{ContextShift, Effect}
import io.dlinov.auth.dao.hikari.ec.BlockingECWrapper
import org.http4s.HttpRoutes
import org.http4s.server.staticcontent.webjarService
import org.http4s.server.staticcontent.WebjarService.{Config, WebjarAsset}

class StaticsRoutes[F[_]](ecWrapper: BlockingECWrapper)(implicit cs: ContextShift[F], effect: Effect[F]) {

  private val ec = ecWrapper.ec
  protected val supportedExtensions: Set[String] = Set(".js", ".css", ".map", ".html", ".webm", ".png")

  val routes: HttpRoutes[F] = webjarService[F](
    Config(
      filter = isWebAsset,
      blockingExecutionContext = ec))

  protected def isWebAsset(asset: WebjarAsset): Boolean = {
    supportedExtensions.exists(asset.asset.endsWith)

  }
}
