package io.dlinov.auth.routes

import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import org.http4s.rho.AuthedContext

trait AuthenticatedRoutes[F[_]] extends Routes[F] {
  val authCtx: AuthedContext[F, BackOfficeUser]
}
