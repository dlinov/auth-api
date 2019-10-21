package io.dlinov.auth.routes.proxied

import cats.effect.{Resource, Sync}
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.routes.Routes
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.rho.RhoRoutes
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.routes.Routes
import io.dlinov.auth.routes.Routes.ApiPrefix

class TypesProxy[F[_]](
    override val httpClientResource: Resource[F, Client[F]],
    override val proxyConfig: ProxyConfig
)(
    implicit override protected val syncF: Sync[F]
) extends Routes[F]
    with ProxySupport[F] {

  protected val routeRoot: String        = ApiPrefix + "/types"
  protected val currenciesRoot: String   = ApiPrefix + "/currencies"
  protected val accountTypesRoot: String = ApiPrefix + "/account_types"

  override def routes: RhoRoutes[F] = new RhoRoutes[F] {
    GET / `routeRoot` / * |>> { (req: Request[F], _: List[String]) ⇒
      proxyRequest(req)
    }

    GET / `currenciesRoot` / * |>> { (req: Request[F], _: List[String]) ⇒
      proxyRequest(req)
    }

    GET / `accountTypesRoot` / * |>> { (req: Request[F], _: List[String]) ⇒
      proxyRequest(req)
    }
  }
}
