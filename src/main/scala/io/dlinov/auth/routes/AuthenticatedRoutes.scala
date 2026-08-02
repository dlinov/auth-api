package io.dlinov.auth.routes

import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import org.http4s.AuthedRoutes

trait AuthenticatedRoutes[F[_]] extends Routes[F] {
  def routes: AuthedRoutes[BackOfficeUser, F]
}
