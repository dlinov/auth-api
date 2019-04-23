package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.BackOfficeUserAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate}
import org.http4s.rho.{AuthedContext, RhoRoutes}
import org.http4s.rho.swagger.syntax.io._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.BackOfficeUserAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate}
import io.dlinov.auth.routes.dto.implicits.Implicits.UserConverter
import io.dlinov.auth.routes.json.EntityDecoders._
import io.dlinov.auth.routes.json.CirceEncoders._

class BackOfficeUserRoutes(
    override val authCtx: AuthedContext[IO, BackOfficeUser],
    algebra: BackOfficeUserAlgebra)
  extends AuthenticatedRoutes[IO] with AuthorizationBehavior {
  import Routes._

  // private val bouRoot: / = ApiPrefix / "back_office_users"
  private val bouRoot = ApiPrefix + "/back_office_users"
  override protected val scopes: Scopes = new Scopes("back_office_users")

  override val routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    "Create new back office user" **
      POST / ApiPrefix / "back_office_users" +?
      RhoExt.reactivateParam >>>
      authCtx.auth ^
      bouToCreateEntityDecoder |>> {
        (maybeReactivate: Option[Boolean], user: BackOfficeUser, newUser: BackOfficeUserToCreate) ⇒
          (for {
            _ ← EitherT(IO(requireCreateAccess(user)))
            //newUser ← req.attemptAs[BackOfficeUserToCreate].leftMap(_.asServiceError)
            resp ← EitherT {
              import newUser._
              algebra.create(
                userName = userName,
                email = email,
                phoneNumber = phoneNumber,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                description = description,
                homePage = homePage,
                activeLanguage = activeLanguage,
                customData = customData.map(_.noSpaces),
                roleId = roleId,
                businessUnitId = businessUnitId,
                createdBy = user.userName,
                maybeReactivate = maybeReactivate)
            }
          } yield resp.asApi)
            .value.flatMap(handleServiceCreateResponse[BackOfficeUserToRead])
      }

    "Get all back office users (optionally paginated)" **
      GET / bouRoot +?
      (RhoExt.bouSearchParams & RhoExt.limitAndOffsetParams) >>>
      authCtx.auth |>> {
        (maybeFirstName: Option[String], maybeLastName: Option[String],
        maybeEmail: Option[String], maybePhoneNumber: Option[String],
        maybeLimit: Option[Int], maybeOffset: Option[Int], user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireReadAccess(user)))
            resp ← EitherT(algebra.findAll(
              maybeLimit = maybeLimit,
              maybeOffset = maybeOffset,
              maybeFirstName = maybeFirstName,
              maybeLastName = maybeLastName,
              maybeEmail = maybeEmail,
              maybePhoneNumber = maybePhoneNumber))
          } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset))
            .value.flatMap(handleServiceResponse[PaginatedResult[BackOfficeUserToRead]])
      }

    "Find back office user by id" **
      GET / `bouRoot` / pathVar[UUID] >>>
      authCtx.auth |>> {
        (id: UUID, user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireReadAccess(user)))
            userOrError ← EitherT(algebra.findById(id))
          } yield userOrError.asApi)
            .value.flatMap(handleServiceResponse[BackOfficeUserToRead])
      }

    "Update back office user by id" **
      PUT / `bouRoot` / pathVar[UUID] >>>
      authCtx.auth ^
      bouToUpdateEntityDecoder |>> {
        (id: UUID, user: BackOfficeUser, userToUpdate: BackOfficeUserToUpdate) ⇒
          (for {
            _ ← EitherT(IO(requireUpdateAccess(user)))
            userOrError ← EitherT {
              import userToUpdate._
              algebra.update(
                id = id,
                email = email,
                phoneNumber = phoneNumber,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                description = description,
                homePage = homePage,
                activeLanguage = activeLanguage,
                customData = customData.map(_.noSpaces),
                roleId = roleId,
                businessUnitId = businessUnitId,
                updatedBy = user.userName)
            }
          } yield userOrError.asApi)
            .value.flatMap(handleServiceResponse[BackOfficeUserToRead])
      }

    "Delete back office user by id" **
      DELETE / `bouRoot` / pathVar[UUID] >>>
      authCtx.auth |>> {
        (id: UUID, user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireDeleteAccess(user)))
            userIdOrError ← EitherT(algebra.remove(id, user.userName))
          } yield userIdOrError).value.flatMap(handleServiceResponse[UUID])
      }

  }
}
