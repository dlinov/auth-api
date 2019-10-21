package io.dlinov.auth.routes

import cats.Applicative
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.flatMap._
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
import io.dlinov.auth.routes.json.{EntityDecoders, EntityEncoders}
import org.http4s.rho.swagger.SwaggerSyntax

class AdminRoutes[F[_]](
    override val authCtx: AuthedContext[F, BackOfficeUser],
    authenticationAlgebra: AuthenticationAlgebra[F]
)(
    implicit
    override val syncF: Sync[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F]
    with EntityEncoders[F]
    with SwaggerSyntax[F] {

  import Routes._

  private val adminRoot = ApiPrefix + "/admin"

  override protected val scopes: Scopes                        = new Scopes("admin")
  implicit override protected def applicativeF: Applicative[F] = syncF

  override def routes: RhoRoutes[F] = new RhoRoutes[F] {
    POST / `adminRoot` / "reset_password" >>>
      authCtx.auth ^
      passwordResetLinkRequestEntityDecoder |>> {
      (req: Request[F], admin: BackOfficeUser, passwordResetRequest: ResetPasswordLinkRequest) ⇒
        (for {
          _ ← EitherT(syncF.delay(requireFullAccess(admin)))
          resp ← EitherT {
            val userName     = passwordResetRequest.userName
            val email        = passwordResetRequest.email
            val maybeCaptcha = passwordResetRequest.captcha
            val maybeReferer = req.headers.get(Referer).map(_.value)
            logger.warn("Referer is " + maybeReferer.getOrElse("missing"))
            authenticationAlgebra.sendPasswordResetLink(userName, email, maybeCaptcha, maybeReferer)
          }
        } yield resp).value.flatMap(handleServiceResponse[Unit])
    }

  }
}
