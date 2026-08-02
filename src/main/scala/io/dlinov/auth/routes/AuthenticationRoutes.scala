package io.dlinov.auth.routes

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.dlinov.auth.domain.algebras.AuthenticationAlgebra
import io.dlinov.auth.routes.dto.{
  BackOfficeUserToRead,
  CredentialsToRead,
  CredentialsToUpdate,
  LoginResponse,
  LoginStatusResponse,
  PasswordReset,
  ResetPasswordLinkRequest
}
import io.dlinov.auth.util.Logging
import org.http4s.headers.Referer
import org.http4s.HttpRoutes
import io.dlinov.auth.routes.dto.implicits.Implicits._
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders
import io.dlinov.auth.routes.Routes.TokenQParam

class AuthenticationRoutes[F[_]](authenticationAlgebra: AuthenticationAlgebra[F])(implicit
    override val asyncF: Async[F]
) extends Routes[F]
    with Logging
    with EntityDecoders[F] {
  import Routes._

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> ApiRoot / "login" =>
      req.as[CredentialsToRead].flatMap { creds =>
        (for {
          loginResp <- EitherT(
            authenticationAlgebra.login(creds.user, creds.password, creds.captcha)
          )
            .map { case (user, token) =>
              LoginResponse(token, user.asApi)
            }
        } yield loginResp).value.flatMap(handleServiceResponse[LoginResponse])
      }

    case req @ PUT -> ApiRoot / "update_password" =>
      req.as[CredentialsToUpdate].flatMap { creds =>
        (for {
          loginResp <- EitherT(
            authenticationAlgebra.updatePassword(creds.user, creds.oldPassword, creds.newPassword)
          ).map { case (user, token) => LoginResponse(token, user.asApi) }
        } yield loginResp).value.flatMap(handleServiceResponse[LoginResponse])
      }

    case req @ POST -> ApiRoot / "reset_password" =>
      req.as[ResetPasswordLinkRequest].flatMap { passwordResetRequest =>
        (for {
          resp <- EitherT {
            val userName     = passwordResetRequest.userName
            val email        = passwordResetRequest.email
            val maybeCaptcha = passwordResetRequest.captcha
            val maybeReferer = req.headers.get[Referer].map(_.uri.renderString)
            logger.info("Referer is " + maybeReferer.getOrElse("missing"))
            authenticationAlgebra.sendPasswordResetLink(userName, email, maybeCaptcha, maybeReferer)
          }
        } yield resp).value.flatMap(handleServiceResponse[Unit])
      }

    case GET -> ApiRoot / "reset_password" :? TokenQParam(token) =>
      for {
        validationResult <- authenticationAlgebra.validatePasswordResetToken(token)
        resp             <- handleServiceResponse[Unit](validationResult)
      } yield resp

    case req @ PUT -> ApiRoot / "reset_password" =>
      req.as[PasswordReset].flatMap { pwdResetForm =>
        (for {
          loginResp <- EitherT(
            authenticationAlgebra.resetPassword(pwdResetForm.password, pwdResetForm.token)
          ).map { case (user, token) => LoginResponse(token, user.asApi) }
        } yield loginResp).value.flatMap(handleServiceResponse[LoginResponse])
      }

    case GET -> ApiRoot / "status" =>
      for {
        loginStatus <- authenticationAlgebra.status
        resp        <- handleServiceResponse[LoginStatusResponse](loginStatus)
      } yield resp

    case req @ GET -> ApiRoot / "validate_token" =>
      (for {
        token <- EitherT(asyncF.delay(extractTokenFromRequest(req)))
        resp  <- EitherT(authenticationAlgebra.validateToken(token).map(_.map(_.asApi)))
      } yield resp).value.flatMap(handleServiceResponse[BackOfficeUserToRead])
  }
}
