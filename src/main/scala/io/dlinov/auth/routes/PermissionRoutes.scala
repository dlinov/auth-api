package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.flatMap._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.PermissionAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{PermissionToCreate, PermissionToRead, PermissionToUpdate}
import org.http4s.rho.{AuthedContext, RhoRoutes}
import io.dlinov.auth.routes.dto.implicits.Implicits.{
  PermissionConverter,
  PermissionToCreateConverter
}
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.PermissionAlgebra
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.dto.{PermissionToCreate, PermissionToRead, PermissionToUpdate}
import io.dlinov.auth.routes.json.EntityDecoders

class PermissionRoutes[F[_]](
    override val authCtx: AuthedContext[F, BackOfficeUser],
    algebra: PermissionAlgebra[F]
)(
    implicit override protected val syncF: Sync[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F] {
  import Routes._

  private val pRoot = ApiPrefix + "/permissions"

  override protected val scopes: Scopes = new Scopes("permissions")

  override val routes: RhoRoutes[F] = new RhoRoutes[F] {
    POST / `pRoot` +?
      rhoExt.reactivateParam >>>
      authCtx.auth ^
      pToCreateEntityDecoder |>> {
      (
          maybeReactivate: Option[Boolean],
          user: BackOfficeUser,
          permissionToCreate: PermissionToCreate
      ) ⇒
        (for {
          _ ← EitherT(Sync[F].delay(requireCreateAccess(user)))
          permissionBlueprint = permissionToCreate.asDomain(user.userName)
          resp ← EitherT(algebra.create(permissionBlueprint, maybeReactivate))
        } yield resp.asApi).value
          .flatMap(handleServiceCreateResponse[PermissionToRead])
    }

    GET / `pRoot` +?
      (rhoExt.buIdParam and rhoExt.roleIdParam and rhoExt.userIdParam and rhoExt.limitAndOffsetParams) >>>
      authCtx.auth |>> {
      (
          buId: UUID,
          roleId: UUID,
          maybeUserId: Option[UUID],
          maybeLimit: Option[Int],
          maybeOffset: Option[Int],
          user: BackOfficeUser
      ) ⇒
        (for {
          _    ← EitherT(Sync[F].delay(requireReadAccess(user)))
          resp ← EitherT(algebra.findBy(buId, roleId, maybeUserId, maybeLimit, maybeOffset))
        } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
          .flatMap(handleServiceResponse[PaginatedResult[PermissionToRead]])
    }

    PUT / `pRoot` / pathVar[UUID] >>>
      authCtx.auth ^
      pToUpdateEntityDecoder |>> { (id: UUID, user: BackOfficeUser, pu: PermissionToUpdate) ⇒
      (for {
        _           ← EitherT(Sync[F].delay(requireUpdateAccess(user)))
        userOrError ← EitherT(algebra.update(id, pu.permissionKey, pu.scopeId, user.userName))
      } yield userOrError.asApi).value
        .flatMap(handleServiceResponse[PermissionToRead])
    }

    DELETE / `pRoot` / pathVar[UUID] >>>
      authCtx.auth |>> { (id: UUID, user: BackOfficeUser) ⇒
      (for {
        _             ← EitherT(Sync[F].delay(requireDeleteAccess(user)))
        userIdOrError ← EitherT(algebra.remove(id, user.userName))
      } yield userIdOrError).value
        .flatMap(handleServiceResponse[UUID])
    }
  }

}
