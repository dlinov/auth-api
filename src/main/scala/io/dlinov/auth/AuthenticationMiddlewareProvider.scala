package io.dlinov.auth

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.Applicative
import cats.data.{EitherT, Kleisli, NonEmptyList, OptionT}
import cats.effect.Sync
import io.circe.syntax._
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.algebras.BackOfficeUserAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, BusinessUnit, Email, Role}
import io.dlinov.auth.routes.dto.ApiError
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, BusinessUnit, Email, Role}
import io.dlinov.auth.routes.json.CirceEncoders.apiErrorEncoder
import io.dlinov.auth.domain.algebras.BackOfficeUserAlgebra
import io.dlinov.auth.routes.dto.ApiError

trait AuthenticationMiddlewareProvider[F[_]]
    extends TokenExtractBehavior[F]
    with AuthenticationBehavior
    with Http4sDsl[F] {

  implicit protected def syncF: Sync[F]

  def backOfficeUserAlgebra: BackOfficeUserAlgebra[F]

  private def retrieveUser: Kleisli[F, String, ServiceResponse[BackOfficeUser]] = {
    Kleisli(id ⇒ backOfficeUserAlgebra.findByName(id))
  }

  private val authUser: Kleisli[F, Request[F], ServiceResponse[BackOfficeUser]] =
    Kleisli({ request ⇒
      if (request.pathInfo == "/swagger.json") {
        Applicative[F]
          .pure[ServiceResponse[BackOfficeUser]](Right(AuthenticationMiddlewareProvider.fakeUser))
      } else {
        (for {
          rawToken ← EitherT(syncF.delay(extractTokenFromRequest(request)))
          c        ← EitherT(syncF.delay(userClaim(rawToken)))
          u        ← EitherT(retrieveUser(c.loggedInAs))
        } yield u).value
      }
    })

  private val onFailure: AuthedRoutes[ServiceError, F] = Kleisli(req ⇒ {
    val error   = req.authInfo
    val id      = error.id
    val code    = error.code
    val message = error.message
    val maybeParams = for {
      f ← error.fieldName
      v ← error.fieldValue
    } yield ApiError.ErrorParams(f, v)
    val responseBody = ApiError(id, code, message, maybeParams).asJson
    val authenticate =
      headers.`WWW-Authenticate`(NonEmptyList.of(Challenge("Bearer", "API access")))
    val resp = Unauthorized(authenticate, responseBody)
    OptionT.liftF(resp)
  })

  val Authenticated: AuthMiddleware[F, BackOfficeUser] = AuthMiddleware(authUser, onFailure)
}

object AuthenticationMiddlewareProvider {
  private val EmptyUUID = new UUID(0L, 0L)

  val SwaggerAnonPath = "anon_swagger.json"

  val fakeUser: BackOfficeUser = {
    val now = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
    BackOfficeUser(
      id = EmptyUUID,
      userName = "SWAGGER",
      email = Email("noreply@foo.bar"),
      phoneNumber = None,
      firstName = "",
      middleName = None,
      lastName = "",
      description = None,
      homePage = None,
      activeLanguage = None,
      customData = None,
      lastLoginTimestamp = None,
      role = Role(EmptyUUID, "SWAGGER", "", "", now, now),
      businessUnit = BusinessUnit(EmptyUUID, "SWAGGER", "", "", now, now),
      permissions = Seq.empty,
      createdBy = "",
      updatedBy = "",
      createdTime = now,
      updatedTime = now
    )
  }
}
