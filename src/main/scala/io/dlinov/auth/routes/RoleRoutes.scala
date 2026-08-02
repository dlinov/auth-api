package io.dlinov.auth.routes

import java.util.UUID

import cats.Applicative
import cats.data.EitherT
import cats.effect.Async
import cats.syntax.flatMap._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.RoleAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{RoleToCreate, RoleToRead, RoleToUpdate}
import org.http4s.AuthedRoutes
import io.dlinov.auth.routes.dto.implicits.Implicits.RoleConverter
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.{EntityDecoders, EntityEncoders}

class RoleRoutes[F[_]](
    algebra: RoleAlgebra[F]
)(implicit
    override protected val asyncF: Async[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F]
    with EntityEncoders[F] {
  import Routes._

  private val rolesRoot                                        = ApiRoot / "roles"
  override protected val scopes                                = new Scopes("roles")
  implicit override protected def applicativeF: Applicative[F] = asyncF

  override val routes: AuthedRoutes[BackOfficeUser, F] = AuthedRoutes.of {
    case req @ POST -> `rolesRoot` :? MaybeReactivateQParam(maybeReactivate) as user =>
      req.req.as[RoleToCreate].flatMap { newRole =>
        (for {
          _    <- EitherT(asyncF.delay(requireCreateAccess(user)))
          resp <- EitherT {
            import newRole._
            algebra.create(name, user.userName, maybeReactivate)
          }
        } yield resp.asApi).value.flatMap(handleServiceCreateResponse[RoleToRead])
      }

    case GET -> `rolesRoot` :? MaybeLimitQParam(maybeLimit) +& MaybeOffsetQParam(
          maybeOffset
        ) as user =>
      (for {
        _    <- EitherT(asyncF.delay(requireReadAccess(user)))
        resp <- EitherT(algebra.findAll(maybeLimit, maybeOffset))
      } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
        .flatMap(handleServiceResponse[PaginatedResult[RoleToRead]])

    case GET -> `rolesRoot` / UUIDVar(id) as user =>
      (for {
        _           <- EitherT(asyncF.delay(requireReadAccess(user)))
        roleOrError <- EitherT(algebra.findById(id))
      } yield roleOrError.asApi).value.flatMap(handleServiceResponse[RoleToRead])

    case req @ PUT -> `rolesRoot` / UUIDVar(id) as user =>
      req.req.as[RoleToUpdate].flatMap { roleToUpdate =>
        (for {
          _           <- EitherT(asyncF.delay(requireUpdateAccess(user)))
          roleOrError <- EitherT(algebra.update(id, roleToUpdate.name, user.userName))
        } yield roleOrError.asApi).value.flatMap(handleServiceResponse[RoleToRead])
      }

    case DELETE -> `rolesRoot` / UUIDVar(id) as user =>
      (for {
        _              <- EitherT(asyncF.delay(requireDeleteAccess(user)))
        scopeIdOrError <- EitherT(algebra.remove(id, user.userName))
      } yield scopeIdOrError).value.flatMap(handleServiceResponse[UUID])
  }

}
