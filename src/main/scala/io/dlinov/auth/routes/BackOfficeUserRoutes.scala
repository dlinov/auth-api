package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.{Async, Sync}
import cats.syntax.flatMap._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.BackOfficeUserAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{
  BackOfficeUserToCreate,
  BackOfficeUserToRead,
  BackOfficeUserToUpdate
}
import org.http4s.AuthedRoutes
import io.dlinov.auth.routes.dto.implicits.Implicits.UserConverter
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders

class BackOfficeUserRoutes[F[_]](
    algebra: BackOfficeUserAlgebra[F]
)(implicit
    override protected val asyncF: Async[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F] {
  import Routes._

  private val bouRoot                   = ApiRoot / "back_office_users"
  override protected val scopes: Scopes = new Scopes("back_office_users")

  override val routes: AuthedRoutes[BackOfficeUser, F] = AuthedRoutes.of {
    // Create new back office user
    case req @ POST -> `bouRoot` :? MaybeReactivateQParam(maybeReactivate) as user =>
      req.req.as[BackOfficeUserToCreate].flatMap { newUser =>
        (for {
          _    <- EitherT(Sync[F].delay(requireCreateAccess(user)))
          resp <- EitherT {
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
              maybeReactivate = maybeReactivate
            )
          }
        } yield resp.asApi).value.flatMap(handleServiceCreateResponse[BackOfficeUserToRead])
      }

    // Get all back office users (optionally paginated, optionally filtered)
    case GET -> `bouRoot` :? MaybeFirstNameQParam(maybeFirstName) +& MaybeLastNameQParam(
          maybeLastName
        ) +& MaybeEmailQParam(maybeEmail) +& MaybePhoneNumberQParam(maybePhoneNumber) +&
        MaybeLimitQParam(maybeLimit) +& MaybeOffsetQParam(maybeOffset) as user =>
      (for {
        _    <- EitherT(Sync[F].delay(requireReadAccess(user)))
        resp <- EitherT(
          algebra.findAll(
            maybeLimit = maybeLimit,
            maybeOffset = maybeOffset,
            maybeFirstName = maybeFirstName,
            maybeLastName = maybeLastName,
            maybeEmail = maybeEmail,
            maybePhoneNumber = maybePhoneNumber
          )
        )
      } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
        .flatMap(handleServiceResponse[PaginatedResult[BackOfficeUserToRead]])

    // Find back office user by id
    case GET -> `bouRoot` / UUIDVar(id) as user =>
      (for {
        _           <- EitherT(Sync[F].delay(requireReadAccess(user)))
        userOrError <- EitherT(algebra.findById(id))
      } yield userOrError.asApi).value.flatMap(handleServiceResponse[BackOfficeUserToRead])

    // Update back office user by id
    case req @ PUT -> `bouRoot` / UUIDVar(id) as user =>
      req.req.as[BackOfficeUserToUpdate].flatMap { userToUpdate =>
        (for {
          _           <- EitherT(Sync[F].delay(requireUpdateAccess(user)))
          userOrError <- EitherT {
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
              updatedBy = user.userName
            )
          }
        } yield userOrError.asApi).value.flatMap(handleServiceResponse[BackOfficeUserToRead])
      }

    // Delete back office user by id
    case DELETE -> `bouRoot` / UUIDVar(id) as user =>
      (for {
        _             <- EitherT(Sync[F].delay(requireDeleteAccess(user)))
        userIdOrError <- EitherT(algebra.remove(id, user.userName))
      } yield userIdOrError).value.flatMap(handleServiceResponse[UUID])
  }
}
