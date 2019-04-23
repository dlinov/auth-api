package io.dlinov.auth.routes.proxied

import cats.effect.{IO, Resource}
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.routes.Routes
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.rho.RhoRoutes
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.routes.Routes
import io.dlinov.auth.routes.Routes.ApiPrefix

class TypesProxy(
    override val httpClientResource: Resource[IO, Client[IO]],
    override val proxyConfig: ProxyConfig)
  extends Routes[IO] with ProxySupport {

  protected val routeRoot: String = ApiPrefix + "/types"
  protected val currenciesRoot: String = ApiPrefix + "/currencies"
  protected val accountTypesRoot: String = ApiPrefix + "/account_types"

  override def routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    GET / `routeRoot` / * |>> {
      (req: Request[IO], _: List[String]) ⇒
        proxyRequest(req)
    }

    GET / `currenciesRoot` / * |>> {
      (req: Request[IO], _: List[String]) ⇒
        proxyRequest(req)
    }

    GET / `accountTypesRoot` / * |>> {
      (req: Request[IO], _: List[String]) ⇒
        proxyRequest(req)
    }
  }
}
