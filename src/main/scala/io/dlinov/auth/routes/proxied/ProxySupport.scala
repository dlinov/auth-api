package io.dlinov.auth.routes.proxied

import java.util.UUID

import cats.effect.Async
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.Routes
import monocle.syntax.all._
import org.http4s.Uri.{Authority, RegName}
import org.http4s.client.Client
import org.http4s.headers.Connection
import org.http4s.{Headers, Request, Response}
import org.typelevel.ci.CIString

trait ProxySupport[F[_]] { self: Routes[F] =>
  implicit protected def asyncF: Async[F]

  def httpClient: Client[F]

  def proxyConfig: ProxyConfig

  /** Forwards `req` to the configured downstream host/port, as user `user`, provided
    * `checkPermissions` grants access; otherwise translates the resulting `ServiceError` into a
    * response the same way regular (non-proxied) routes do.
    */
  def proxyAuthedRequest(
      checkPermissions: BackOfficeUser => Either[ServiceError, Unit]
  )(req: Request[F], user: BackOfficeUser): F[Response[F]] = {
    asyncF
      .delay(checkPermissions(user))
      .attempt
      .flatMap(
        _.fold(
          exc => handleError(ServiceError.unknownError(UUID.randomUUID(), exc.getMessage)),
          _.fold(handleError, _ => forward(req, user.userName))
        )
      )
  }

  private def forward(req: Request[F], userName: String): F[Response[F]] = {
    // The destination host/port for the proxied request is always derived from the rewritten
    // URI below (never trusted from the incoming request), and the client-supplied
    // Authorization/Host/hop-by-hop headers are stripped; X-UserName is then overwritten
    // (rather than merely appended) with the authenticated caller's user name, so a spoofed
    // client-supplied X-UserName header can never reach the downstream service.
    val changedUri = req.uri
      .focus(_.authority)
      .modify(mbAuthority =>
        Some(
          Authority(
            host = RegName(proxyConfig.host),
            port = Some(proxyConfig.port),
            userInfo = mbAuthority.flatMap(_.userInfo)
          )
        )
      )
    val preparedHeaders = ProxySupport
      .sanitizeRequestHeaders(req.headers)
      .put("X-UserName" -> userName)
    val preparedRequest = req.withUri(changedUri).withHeaders(preparedHeaders)
    logger.debug(s"forwarding request to $proxyConfig ${req.uri}")
    // The acquire (connect + send request + receive headers) and the subsequent attachment of
    // the release action to the response body must happen atomically with respect to
    // cancellation (e.g. from the server's request `Timeout`): if the fiber were cancelled after
    // `.allocated` acquired the connection but before its `release` was wired up as the body's
    // finalizer, `release` would never be invoked and the connection would leak. `poll` re-opens
    // cancellation only around the acquire itself (so a slow/hanging downstream connect can
    // still be timed out/cancelled - and `Resource`'s own cancellation handling guarantees a
    // resource that never finished acquiring needs no release), while attaching the finalizer
    // immediately afterwards runs uninterruptibly. The returned body `Stream` itself is left
    // fully cancelable: once returned, an interruption while streaming it out still runs
    // `onFinalizeWeak`'s release exactly once, same as normal completion or a downstream error.
    asyncF.uncancelable { poll =>
      poll(httpClient.run(preparedRequest).allocated).flatMap { case (resp, release) =>
        asyncF
          .delay {
            resp
              .withHeaders(ProxySupport.sanitizeResponseHeaders(resp.headers))
              .copy(body = resp.body.onFinalizeWeak(release))
          }
          .onError(_ => release)
      }
    }
  }
}

object ProxySupport {

  /** Headers that must never be forwarded to the downstream service, regardless of direction:
    * connection-management ("hop-by-hop") headers that are only meaningful between a client and the
    * proxy immediately in front of it, per RFC 7230 6.1 / RFC 9110 7.6.1.
    */
  private val FixedHopByHopHeaders: Set[CIString] = Set(
    CIString("Connection"),
    CIString("Keep-Alive"),
    CIString("Proxy-Authenticate"),
    CIString("Proxy-Authorization"),
    CIString("TE"),
    CIString("Trailer"),
    CIString("Transfer-Encoding"),
    CIString("Upgrade")
  )

  private val RequestOnlyStrippedHeaders: Set[CIString] =
    Set(CIString("Authorization"), CIString("Host"))

  /** Any extra header names listed by a `Connection` header's value are themselves hop-by-hop for
    * this particular request/response and must be stripped along with the fixed set above.
    */
  private def connectionNamedHeaders(headers: Headers): Set[CIString] =
    headers.get[Connection].fold(Set.empty[CIString])(_.values.toList.toSet)

  def sanitizeRequestHeaders(headers: Headers): Headers = {
    val toStrip = FixedHopByHopHeaders ++ RequestOnlyStrippedHeaders ++
      connectionNamedHeaders(headers)
    headers.transform(_.filterNot(h => toStrip.contains(h.name)))
  }

  def sanitizeResponseHeaders(headers: Headers): Headers = {
    val toStrip = FixedHopByHopHeaders ++ connectionNamedHeaders(headers)
    headers.transform(_.filterNot(h => toStrip.contains(h.name)))
  }
}
