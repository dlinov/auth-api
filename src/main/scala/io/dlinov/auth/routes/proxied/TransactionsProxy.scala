package io.dlinov.auth.routes.proxied

import cats.effect.{Resource, Sync}
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import org.http4s.client.Client
import org.http4s.rho.AuthedContext
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.Routes.ApiPrefix

class TransactionsProxy[F[_]](
    override val authCtx: AuthedContext[F, BackOfficeUser],
    override val httpClientResource: Resource[F, Client[F]],
    override val proxyConfig: ProxyConfig
)(implicit override protected val syncF: Sync[F])
    extends ProxiedRoute[F] {

  override protected val routeRoot: String = ApiPrefix + "/transactions"

  override protected def scopes: Scopes = new Scopes("transactions")
}
