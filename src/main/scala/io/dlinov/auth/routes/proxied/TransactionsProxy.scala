package io.dlinov.auth.routes.proxied

import cats.effect.Async
import io.dlinov.auth.AppConfig.ProxyConfig
import io.dlinov.auth.domain.auth.entities.Scopes
import org.http4s.client.Client
import io.dlinov.auth.routes.Routes.ApiPrefix

class TransactionsProxy[F[_]](
    override val httpClient: Client[F],
    override val proxyConfig: ProxyConfig
)(implicit override protected val asyncF: Async[F])
    extends ProxiedRoute[F] {

  override protected val routeRoot: String = ApiPrefix + "/transactions"

  override protected def scopes: Scopes = new Scopes("transactions")
}
