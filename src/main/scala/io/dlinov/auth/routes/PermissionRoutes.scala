package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.{Async, Sync}
import cats.syntax.flatMap._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.PermissionAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{PermissionToCreate, PermissionToRead, PermissionToUpdate}
import org.http4s.AuthedRoutes
import io.dlinov.auth.routes.dto.implicits.Implicits.{
  PermissionConverter,
  PermissionToCreateConverter
}
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders

class PermissionRoutes[F[_]](
    algebra: PermissionAlgebra[F]
)(implicit
    override protected val asyncF: Async[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F] {
  import Routes._

  private val pRoot = ApiRoot / "permissions"

  override protected val scopes: Scopes = new Scopes("permissions")

  override val routes: AuthedRoutes[BackOfficeUser, F] = AuthedRoutes.of {
    case req @ POST -> `pRoot` :? MaybeReactivateQParam(maybeReactivate) as user =>
      req.req.as[PermissionToCreate].flatMap { permissionToCreate =>
        (for {
          _ <- EitherT(Sync[F].delay(requireCreateAccess(user)))
          permissionBlueprint = permissionToCreate.asDomain(user.userName)
          resp <- EitherT(algebra.create(permissionBlueprint, maybeReactivate))
        } yield resp.asApi).value
          .flatMap(handleServiceCreateResponse[PermissionToRead])
      }

    case GET -> `pRoot` :? BUIdQParam(buId) +& RoleIdQParam(roleId) +& MaybeUserIdQParam(
          maybeUserId
        ) +& MaybeLimitQParam(maybeLimit) +& MaybeOffsetQParam(maybeOffset) as user =>
      (for {
        _    <- EitherT(Sync[F].delay(requireReadAccess(user)))
        resp <- EitherT(algebra.findBy(buId, roleId, maybeUserId, maybeLimit, maybeOffset))
      } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
        .flatMap(handleServiceResponse[PaginatedResult[PermissionToRead]])

    case req @ PUT -> `pRoot` / UUIDVar(id) as user =>
      req.req.as[PermissionToUpdate].flatMap { pu =>
        (for {
          _           <- EitherT(Sync[F].delay(requireUpdateAccess(user)))
          userOrError <- EitherT(algebra.update(id, pu.permissionKey, pu.scopeId, user.userName))
        } yield userOrError.asApi).value
          .flatMap(handleServiceResponse[PermissionToRead])
      }

    case DELETE -> `pRoot` / UUIDVar(id) as user =>
      (for {
        _             <- EitherT(Sync[F].delay(requireDeleteAccess(user)))
        userIdOrError <- EitherT(algebra.remove(id, user.userName))
      } yield userIdOrError).value
        .flatMap(handleServiceResponse[UUID])
  }

}
