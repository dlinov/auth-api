package io.dlinov.auth.routes

import cats.Applicative
import cats.data.EitherT
import cats.effect.Async
import cats.syntax.flatMap._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.algebras.AuthenticationAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.ResetPasswordLinkRequest
import org.http4s.AuthedRoutes
import org.http4s.headers.Referer
import io.dlinov.auth.routes.json.{EntityDecoders, EntityEncoders}

class AdminRoutes[F[_]](
    authenticationAlgebra: AuthenticationAlgebra[F]
)(implicit
    override val asyncF: Async[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F]
    with EntityEncoders[F] {

  import Routes._

  implicit override protected def applicativeF: Applicative[F] = asyncF
  override protected val scopes: Scopes                        = new Scopes("admin")

  override val routes: AuthedRoutes[BackOfficeUser, F] = AuthedRoutes.of {
    case req @ POST -> ApiRoot / "admin" / "reset_password" as admin =>
      req.req.as[ResetPasswordLinkRequest].flatMap { passwordResetRequest =>
        (for {
          _    <- EitherT(asyncF.delay(requireFullAccess(admin)))
          resp <- EitherT {
            val userName     = passwordResetRequest.userName
            val email        = passwordResetRequest.email
            val maybeCaptcha = passwordResetRequest.captcha
            val maybeReferer = req.req.headers.get[Referer].map(_.uri.renderString)
            logger.warn("Referer is " + maybeReferer.getOrElse("missing"))
            authenticationAlgebra.sendPasswordResetLink(userName, email, maybeCaptcha, maybeReferer)
          }
        } yield resp).value.flatMap(handleServiceResponse[Unit])
      }
  }
}
