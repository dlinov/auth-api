package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.PermissionAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{PermissionToCreate, PermissionToRead, PermissionToUpdate}
import org.http4s.rho.{AuthedContext, RhoRoutes}
import io.dlinov.auth.routes.dto.implicits.Implicits.{PermissionConverter, PermissionToCreateConverter}
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.PermissionAlgebra
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders._
import io.dlinov.auth.routes.dto.{PermissionToCreate, PermissionToRead, PermissionToUpdate}

class PermissionRoutes(
    override val authCtx: AuthedContext[IO, BackOfficeUser],
    algebra: PermissionAlgebra)
  extends AuthenticatedRoutes[IO] with AuthorizationBehavior {
  import Routes._

  private val pRoot = ApiPrefix + "/permissions"

  override protected val scopes: Scopes = new Scopes("permissions")

  override val routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    POST / `pRoot` +?
      RhoExt.reactivateParam >>>
      authCtx.auth ^
      pToCreateEntityDecoder |>> {
        (maybeReactivate: Option[Boolean], user: BackOfficeUser, permissionToCreate: PermissionToCreate) ⇒
          (for {
            _ ← EitherT(IO(requireCreateAccess(user)))
            permissionBlueprint = permissionToCreate.asDomain(user.userName)
            resp ← EitherT(algebra.create(permissionBlueprint, maybeReactivate))
          } yield resp.asApi).value.flatMap(handleServiceCreateResponse[PermissionToRead])
      }

    GET / `pRoot` +?
      (RhoExt.buIdParam and RhoExt.roleIdParam and RhoExt.userIdParam and RhoExt.limitAndOffsetParams) >>>
      authCtx.auth |>> {
        (buId: UUID,
        roleId: UUID,
        maybeUserId: Option[UUID],
        maybeLimit: Option[Int],
        maybeOffset: Option[Int],
        user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireReadAccess(user)))
            resp ← EitherT(algebra.findBy(buId, roleId, maybeUserId, maybeLimit, maybeOffset))
          } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset))
            .value.flatMap(handleServiceResponse[PaginatedResult[PermissionToRead]])
      }

    PUT / `pRoot` / pathVar[UUID] >>>
      authCtx.auth ^
      pToUpdateEntityDecoder |>> {
        (id: UUID, user: BackOfficeUser, pu: PermissionToUpdate) ⇒
          (for {
            _ ← EitherT(IO(requireUpdateAccess(user)))
            userOrError ← EitherT(algebra.update(id, pu.permissionKey, pu.scopeId, user.userName))
          } yield userOrError.asApi).value.flatMap(handleServiceResponse[PermissionToRead])
      }

    DELETE / `pRoot` / pathVar[UUID] >>>
      authCtx.auth |>> {
        (id: UUID, user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireDeleteAccess(user)))
            userIdOrError ← EitherT(algebra.remove(id, user.userName))
          } yield userIdOrError).value.flatMap(handleServiceResponse[UUID])
      }
  }

}
