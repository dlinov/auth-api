package io.dlinov.auth.routes.proxied

import cats.effect.Async
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.{AuthenticatedRoutes, Routes}
import io.dlinov.auth.routes.Routes.ApiPrefix
import org.http4s.AuthedRoutes
import org.http4s.client.Client

/** Proxies read-only reference data (types/currencies/account_types) to the downstream service.
  *
  * Unlike `ProxiedRoute` (a single root, all 4 CRUD-shaped methods, per-method permission), this
  * fronts three distinct GET-only roots behind a single read-permission check, since none of them
  * support mutation. It was previously mounted as a fully anonymous/unauthenticated route; it now
  * requires authentication and the `types` read permission like every other proxy, so there is no
  * public wildcard proxy endpoint left in the service.
  */
class TypesProxy[F[_]](
    override val httpClient: Client[F],
    override val proxyConfig: ProxyConfig
)(implicit
    override protected val asyncF: Async[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with ProxySupport[F] {

  protected val routeRoot: String        = ApiPrefix + "/types"
  protected val currenciesRoot: String   = ApiPrefix + "/currencies"
  protected val accountTypesRoot: String = ApiPrefix + "/account_types"

  override protected def scopes: Scopes = new Scopes("types")

  private def isUnderAnyRoot(path: org.http4s.Uri.Path): Boolean =
    Routes.isPathUnderRoot(routeRoot)(path) ||
      Routes.isPathUnderRoot(currenciesRoot)(path) ||
      Routes.isPathUnderRoot(accountTypesRoot)(path)

  override def routes: AuthedRoutes[BackOfficeUser, F] =
    AuthedRoutes.of {
      case req @ GET -> path as user if isUnderAnyRoot(path) =>
        proxyAuthedRequest(requireReadAccess)(req.req, user)
    }
}
