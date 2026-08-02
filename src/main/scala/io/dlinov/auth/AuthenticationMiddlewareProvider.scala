package io.dlinov.auth

import cats.data.{EitherT, Kleisli, NonEmptyList, OptionT}
import cats.effect.Async
import io.circe.syntax._
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.algebras.BackOfficeUserAlgebra
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.dto.ApiError
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.routes.json.CirceEncoders.apiErrorEncoder

trait AuthenticationMiddlewareProvider[F[_]]
    extends TokenExtractBehavior[F]
    with AuthenticationBehavior
    with Http4sDsl[F] {

  implicit protected def asyncF: Async[F]

  def backOfficeUserAlgebra: BackOfficeUserAlgebra[F]

  private def retrieveUser: Kleisli[F, String, ServiceResponse[BackOfficeUser]] = {
    Kleisli(id => backOfficeUserAlgebra.findByName(id))
  }

  private val authUser: Kleisli[F, Request[F], ServiceResponse[BackOfficeUser]] =
    Kleisli({ request =>
      (for {
        rawToken <- EitherT(asyncF.delay(extractTokenFromRequest(request)))
        c        <- EitherT(asyncF.delay(userClaim(rawToken)))
        u        <- EitherT(retrieveUser(c.loggedInAs))
      } yield u).value
    })

  private val onFailure: AuthedRoutes[ServiceError, F] = Kleisli(req => {
    val error       = req.context
    val id          = error.id
    val code        = error.code
    val message     = error.message
    val maybeParams = for {
      f <- error.fieldName
      v <- error.fieldValue
    } yield ApiError.ErrorParams(f, v)
    val responseBody = ApiError(id, code, message, maybeParams).asJson
    val authenticate =
      headers.`WWW-Authenticate`(NonEmptyList.of(Challenge("Bearer", "API access")))
    val resp = Unauthorized(authenticate, responseBody)
    OptionT.liftF(resp)
  })

  val Authenticated: AuthMiddleware[F, BackOfficeUser] = AuthMiddleware(authUser, onFailure)
}
