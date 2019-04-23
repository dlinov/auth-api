package io.dlinov.auth.routes.proxied

import cats.effect.{IO, Resource}
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import org.http4s.client.Client
import org.http4s.rho.AuthedContext
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.Routes.ApiPrefix

class AccountsProxy(
    override val authCtx: AuthedContext[IO, BackOfficeUser],
    override val httpClientResource: Resource[IO, Client[IO]],
    override val proxyConfig: ProxyConfig)
  extends ProxiedRoute {

  override protected val routeRoot: String = ApiPrefix + "/accounts"

  override protected def scopes: Scopes = new Scopes("accounts")
}
