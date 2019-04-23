package io.dlinov.auth.routes

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.algebras.AuthenticationAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.ResetPasswordLinkRequest
import org.http4s.Request
import org.http4s.headers.Referer
import org.http4s.rho.{AuthedContext, RhoRoutes}
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.algebras.AuthenticationAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.ResetPasswordLinkRequest
import io.dlinov.auth.routes.json.EntityDecoders._

class AdminRoutes(
    override val authCtx: AuthedContext[IO, BackOfficeUser],
    authenticationAlgebra: AuthenticationAlgebra)
  extends AuthenticatedRoutes[IO] with AuthorizationBehavior {

  import Routes._

  private val adminRoot = ApiPrefix + "/admin"

  override protected val scopes: Scopes = new Scopes("admin")

  override def routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    POST / `adminRoot` / "reset_password" >>>
      authCtx.auth ^
      passwordResetLinkRequestEntityDecoder |>> {
        (req: Request[IO], admin: BackOfficeUser, passwordResetRequest: ResetPasswordLinkRequest) ⇒
          (for {
            _ ← EitherT(IO(requireFullAccess(admin)))
            resp ← EitherT {
              val userName = passwordResetRequest.userName
              val email = passwordResetRequest.email
              val maybeCaptcha = passwordResetRequest.captcha
              val maybeReferer = req.headers.get(Referer).map(_.value)
              logger.warn("Referer is " + maybeReferer.getOrElse("missing"))
              authenticationAlgebra.sendPasswordResetLink(userName, email, maybeCaptcha, maybeReferer)
            }
          } yield resp).value.flatMap(handleServiceResponse[Unit])
      }

  }
}
