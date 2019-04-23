package io.dlinov.auth.routes.proxied

import java.util.UUID

import cats.effect.{IO, Resource}
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.Routes
import monocle.macros.syntax.lens._
import org.http4s.Uri.{Authority, RegName}
import org.http4s.client.Client
import org.http4s.{Header, Request, Response}
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.Routes

trait ProxySupport { self: Routes[IO] ⇒
  def httpClientResource: Resource[IO, Client[IO]]

  def proxyConfig: ProxyConfig

  def proxyRequest(req: Request[IO]): IO[Response[IO]] = {
    httpClientResource.use { httpClient ⇒
      val changedUri = req.uri
        .lens(_.authority)
        .modify(mbAuthority ⇒ Some(Authority(
          host = RegName(proxyConfig.host),
          port = Some(proxyConfig.port),
          userInfo = mbAuthority.flatMap(_.userInfo))))
      val preparedRequest = req
        .withUri(changedUri)
      httpClient.fetch(preparedRequest)(r ⇒ {
        logger.debug(s"forwarding request to $proxyConfig ${req.uri}")
        IO(Response(
          status = r.status,
          httpVersion = r.httpVersion,
          headers = r.headers,
          body = r.body,
          attributes = r.attributes))
      })
    }
  }

  def proxyAuthedRequest(checkPermissions: BackOfficeUser ⇒ Either[ServiceError, Unit])(req: Request[IO], user: BackOfficeUser): IO[Response[IO]] = {
    IO(checkPermissions(user))
      .attempt
      .flatMap(_
        .fold(
          exc ⇒ handleError(ServiceError.unknownError(UUID.randomUUID(), exc.getMessage)),
          _.fold(
            handleError,
            _ ⇒ {
              httpClientResource.use(httpClient ⇒ {
                val changedUri = req.uri
                  .lens(_.authority)
                  .modify(mbAuthority ⇒ Some(Authority(
                    host = RegName(proxyConfig.host),
                    port = Some(proxyConfig.port),
                    userInfo = mbAuthority.flatMap(_.userInfo))))
                val preparedRequest = req
                  .withUri(changedUri)
                  .withHeaders(req.headers.put(Header("X-UserName", user.userName)))
                httpClient.fetch(preparedRequest)(r ⇒ {
                  logger.debug(s"forwarding request to $proxyConfig ${req.uri}")
                  IO(Response(
                    status = r.status,
                    httpVersion = r.httpVersion,
                    headers = r.headers,
                    body = r.body,
                    attributes = r.attributes))
                })
              })
            })))
  }
}
