package io.dlinov.auth.routes

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.domain.algebras.AuthenticationAlgebra
import io.dlinov.auth.routes.dto.{BackOfficeUserToRead, CredentialsToRead, CredentialsToUpdate, LoginResponse, LoginStatusResponse, PasswordReset, ResetPasswordLinkRequest}
import io.dlinov.auth.util.Logging
import org.http4s.Request
import org.http4s.headers.{Authorization, Referer}
import org.http4s.rho.RhoRoutes
import io.dlinov.auth.domain.algebras.AuthenticationAlgebra
import io.dlinov.auth.util.Logging
import io.dlinov.auth.routes.dto.implicits.Implicits._
import io.dlinov.auth.routes.dto._
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders._

class AuthenticationRoutes(authenticationAlgebra: AuthenticationAlgebra)
  extends Routes[IO] with Logging {
  import Routes._

  override val routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    POST / ApiPrefix / "login" ^ credentialsToReadEntityDecoder |>> { creds: CredentialsToRead ⇒
      (for {
        loginResp ← EitherT(authenticationAlgebra.login(creds.user, creds.password, creds.captcha))
          .map {
            case (user, token) ⇒ LoginResponse(token, user.asApi)
          }
      } yield loginResp).value.flatMap(handleServiceResponse[LoginResponse])
    }

    PUT / ApiPrefix / "update_password" ^ credentialsToUpdateEntityDecoder |>> { creds: CredentialsToUpdate ⇒
      (for {
        loginResp ← EitherT(authenticationAlgebra.updatePassword(creds.user, creds.oldPassword, creds.newPassword))
          .map { case (user, token) ⇒ LoginResponse(token, user.asApi) }
      } yield loginResp).value.flatMap(handleServiceResponse[LoginResponse])
    }

    POST / ApiPrefix / "reset_password" ^ passwordResetLinkRequestEntityDecoder |>> {
      (req: Request[IO], passwordResetRequest: ResetPasswordLinkRequest) ⇒
        (for {
          resp ← EitherT {
            val userName = passwordResetRequest.userName
            val email = passwordResetRequest.email
            val maybeCaptcha = passwordResetRequest.captcha
            val maybeReferer = req.headers.get(Referer).map(_.value)
            logger.info("Referer is " + maybeReferer.getOrElse("missing"))
            authenticationAlgebra.sendPasswordResetLink(userName, email, maybeCaptcha, maybeReferer)
          }
        } yield resp).value.flatMap(handleServiceResponse[Unit])
    }

    GET / ApiPrefix / "reset_password" +? param[String]("token") |>> { token: String ⇒
      for {
        validationResult ← authenticationAlgebra.validatePasswordResetToken(token)
        resp ← handleServiceResponse[Unit](validationResult)
      } yield resp
    }

    PUT / ApiPrefix / "reset_password" ^ passwordResetEntityDecoder |>> { pwdResetForm: PasswordReset ⇒
      (for {
        loginResp ← EitherT(authenticationAlgebra.resetPassword(pwdResetForm.password, pwdResetForm.token))
          .map { case (user, token) ⇒ LoginResponse(token, user.asApi) }
      } yield loginResp).value.flatMap(handleServiceResponse[LoginResponse])
    }

    GET / ApiPrefix / "status" |>> {
      for {
        loginStatus ← authenticationAlgebra.status
        resp ← handleServiceResponse[LoginStatusResponse](loginStatus)
      } yield resp
    }

    GET / ApiPrefix / "validate_token" >>> capture(Authorization) |>> { authH: Authorization.HeaderT ⇒
      (for {
        token ← EitherT(IO(extractTokenFromHeader(authH)))
        resp ← EitherT(authenticationAlgebra.validateToken(token).map(_.map(_.asApi)))
      } yield resp).value.flatMap(handleServiceResponse[BackOfficeUserToRead])
    }
  }
}
