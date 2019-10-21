package io.dlinov.auth.routes.proxied

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
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

trait ProxySupport[F[_]] { self: Routes[F] ⇒
  implicit protected def syncF: Sync[F]

  def httpClientResource: Resource[F, Client[F]]

  def proxyConfig: ProxyConfig

  def proxyRequest(req: Request[F]): F[Response[F]] = {
    httpClientResource.use { httpClient ⇒
      val changedUri = req.uri
        .lens(_.authority)
        .modify(
          mbAuthority ⇒
            Some(
              Authority(
                host = RegName(proxyConfig.host),
                port = Some(proxyConfig.port),
                userInfo = mbAuthority.flatMap(_.userInfo)
              )
            )
        )
      val preparedRequest = req
        .withUri(changedUri)
      httpClient.fetch(preparedRequest)(r ⇒ {
        logger.debug(s"forwarding request to $proxyConfig ${req.uri}")
        // TODO: replace .pure with something better IO {} / IO.pure
        syncF.delay(
          Response(
            status = r.status,
            httpVersion = r.httpVersion,
            headers = r.headers,
            body = r.body,
            attributes = r.attributes
          )
        )
      })
    }
  }

  def proxyAuthedRequest(
      checkPermissions: BackOfficeUser ⇒ Either[ServiceError, Unit]
  )(req: Request[F], user: BackOfficeUser): F[Response[F]] = {

    syncF
      .delay(checkPermissions(user))
      .attempt
      .flatMap(
        _.fold(
          exc ⇒ handleError(ServiceError.unknownError(UUID.randomUUID(), exc.getMessage)),
          _.fold(
            handleError,
            _ ⇒ {
              httpClientResource.use(httpClient ⇒ {
                val changedUri = req.uri
                  .lens(_.authority)
                  .modify(
                    mbAuthority ⇒
                      Some(
                        Authority(
                          host = RegName(proxyConfig.host),
                          port = Some(proxyConfig.port),
                          userInfo = mbAuthority.flatMap(_.userInfo)
                        )
                      )
                  )
                val preparedRequest = req
                  .withUri(changedUri)
                  .withHeaders(req.headers.put(Header("X-UserName", user.userName)))
                httpClient.fetch(preparedRequest)(r ⇒ {
                  logger.debug(s"forwarding request to $proxyConfig ${req.uri}")
                  syncF.delay(
                    Response(
                      status = r.status,
                      httpVersion = r.httpVersion,
                      headers = r.headers,
                      body = r.body,
                      attributes = r.attributes
                    )
                  )
                })
              })
            }
          )
        )
      )
  }
}
