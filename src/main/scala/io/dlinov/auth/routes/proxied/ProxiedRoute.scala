package io.dlinov.auth.routes.proxied

import cats.effect.IO
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.AuthenticatedRoutes
import org.http4s.Request
import org.http4s.rho.RhoRoutes
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.routes.AuthenticatedRoutes

trait ProxiedRoute
  extends AuthenticatedRoutes[IO] with AuthorizationBehavior with ProxySupport {

  protected val routeRoot: String

  override def routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    GET / `routeRoot` / * >>>
      authCtx.auth |>> {
        (req: Request[IO], _: List[String], user: BackOfficeUser) ⇒
          proxyAuthedRequest(requireReadAccess)(req, user)
      }

    POST / `routeRoot` / * >>>
      authCtx.auth |>> {
        (req: Request[IO], _: List[String], user: BackOfficeUser) ⇒
          proxyAuthedRequest(requireCreateAccess)(req, user)
      }

    PUT / `routeRoot` / * >>>
      authCtx.auth |>> {
        (req: Request[IO], _: List[String], user: BackOfficeUser) ⇒
          proxyAuthedRequest(requireUpdateAccess)(req, user)
      }

    DELETE / `routeRoot` / * >>>
      authCtx.auth |>> {
        (req: Request[IO], _: List[String], user: BackOfficeUser) ⇒
          proxyAuthedRequest(requireDeleteAccess)(req, user)
      }
  }

}
