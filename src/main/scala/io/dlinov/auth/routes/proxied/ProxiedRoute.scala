package io.dlinov.auth.routes.proxied

import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.{AuthenticatedRoutes, Routes}
import org.http4s.AuthedRoutes

trait ProxiedRoute[F[_]]
    extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with ProxySupport[F] {

  protected val routeRoot: String

  private def isUnderRoot(path: org.http4s.Uri.Path): Boolean =
    Routes.isPathUnderRoot(routeRoot)(path)

  override def routes: AuthedRoutes[BackOfficeUser, F] = AuthedRoutes.of {
    case req @ GET -> path as user if isUnderRoot(path) =>
      proxyAuthedRequest(requireReadAccess)(req.req, user)

    case req @ POST -> path as user if isUnderRoot(path) =>
      proxyAuthedRequest(requireCreateAccess)(req.req, user)

    case req @ PUT -> path as user if isUnderRoot(path) =>
      proxyAuthedRequest(requireUpdateAccess)(req.req, user)

    case req @ DELETE -> path as user if isUnderRoot(path) =>
      proxyAuthedRequest(requireDeleteAccess)(req.req, user)
  }

}
