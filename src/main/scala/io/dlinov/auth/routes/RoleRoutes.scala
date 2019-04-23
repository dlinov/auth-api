package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.RoleAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{RoleToCreate, RoleToRead, RoleToUpdate}
import org.http4s.rho.{AuthedContext, RhoRoutes}
import io.dlinov.auth.routes.dto.implicits.Implicits.RoleConverter
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.RoleAlgebra
import io.dlinov.auth.routes.dto.{RoleToCreate, RoleToRead, RoleToUpdate}
import io.dlinov.auth.routes.json.EntityDecoders._
import io.dlinov.auth.routes.json.CirceEncoders._

class RoleRoutes(
    override val authCtx: AuthedContext[IO, BackOfficeUser],
    algebra: RoleAlgebra)
  extends AuthenticatedRoutes[IO] with AuthorizationBehavior {
  import Routes._

  private val rolesRoot = ApiPrefix + "/roles"
  override protected val scopes = new Scopes("roles")

  override val routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    POST / `rolesRoot` +?
      RhoExt.reactivateParam >>>
      authCtx.auth ^
      roleToCreateEntityDecoder |>> {
        (maybeReactivate: Option[Boolean], user: BackOfficeUser, newRole: RoleToCreate) ⇒
          (for {
            _ ← EitherT(IO(requireCreateAccess(user)))
            resp ← EitherT {
              import newRole._
              algebra.create(name, user.userName, maybeReactivate)
            }
          } yield resp.asApi).value.flatMap(handleServiceCreateResponse[RoleToRead])
      }

    GET / `rolesRoot` +?
      RhoExt.limitAndOffsetParams >>>
      authCtx.auth |>> {
        (maybeLimit: Option[Int], maybeOffset: Option[Int], user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireReadAccess(user)))
            resp ← EitherT(algebra.findAll(maybeLimit, maybeOffset))
          } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset))
            .value.flatMap(handleServiceResponse[PaginatedResult[RoleToRead]])
      }

    GET / `rolesRoot` / pathVar[UUID] >>>
      authCtx.auth |>> {
        (id: UUID, user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireReadAccess(user)))
            roleOrError ← EitherT(algebra.findById(id))
          } yield roleOrError.asApi).value.flatMap(handleServiceResponse[RoleToRead])
      }

    PUT / `rolesRoot` / pathVar[UUID] >>>
      authCtx.auth ^
      roleToUpdateEntityDecoder |>> {
        (id: UUID, user: BackOfficeUser, roleToUpdate: RoleToUpdate) ⇒
          (for {
            _ ← EitherT(IO(requireUpdateAccess(user)))
            roleOrError ← EitherT(algebra.update(id, roleToUpdate.name, user.userName))
          } yield roleOrError.asApi).value.flatMap(handleServiceResponse[RoleToRead])
      }

    DELETE / `rolesRoot` / pathVar[UUID] >>> authCtx.auth |>> {
      (id: UUID, user: BackOfficeUser) ⇒
        (for {
          _ ← EitherT(IO(requireDeleteAccess(user)))
          scopeIdOrError ← EitherT(algebra.remove(id, user.userName))
        } yield scopeIdOrError).value.flatMap(handleServiceResponse[UUID])
    }
  }

}
