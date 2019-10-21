package io.dlinov.auth.routes

import java.util.UUID

import cats.Applicative
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.flatMap._
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
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.{EntityDecoders, EntityEncoders}
import org.http4s.rho.swagger.SwaggerSyntax

class RoleRoutes[F[_]](
    override val authCtx: AuthedContext[F, BackOfficeUser],
    algebra: RoleAlgebra[F]
)(
    implicit override protected val syncF: Sync[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F]
    with EntityEncoders[F]
    with SwaggerSyntax[F] {
  import Routes._

  private val rolesRoot                                        = ApiPrefix + "/roles"
  override protected val scopes                                = new Scopes("roles")
  implicit override protected def applicativeF: Applicative[F] = syncF

  override val routes: RhoRoutes[F] = new RhoRoutes[F] {
    "Create role" **
      POST / `rolesRoot` +?
      rhoExt.reactivateParam >>>
      authCtx.auth ^
      roleToCreateEntityDecoder |>> {
      (maybeReactivate: Option[Boolean], user: BackOfficeUser, newRole: RoleToCreate) ⇒
        (for {
          _ ← EitherT(syncF.delay(requireCreateAccess(user)))
          resp ← EitherT {
            import newRole._
            algebra.create(name, user.userName, maybeReactivate)
          }
        } yield resp.asApi).value.flatMap(handleServiceCreateResponse[RoleToRead])
    }

    GET / `rolesRoot` +?
      rhoExt.limitAndOffsetParams >>>
      authCtx.auth |>> { (maybeLimit: Option[Int], maybeOffset: Option[Int], user: BackOfficeUser) ⇒
      (for {
        _    ← EitherT(syncF.delay(requireReadAccess(user)))
        resp ← EitherT(algebra.findAll(maybeLimit, maybeOffset))
      } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
        .flatMap(handleServiceResponse[PaginatedResult[RoleToRead]])
    }

    GET / `rolesRoot` / pathVar[UUID] >>>
      authCtx.auth |>> { (id: UUID, user: BackOfficeUser) ⇒
      (for {
        _           ← EitherT(syncF.delay(requireReadAccess(user)))
        roleOrError ← EitherT(algebra.findById(id))
      } yield roleOrError.asApi).value.flatMap(handleServiceResponse[RoleToRead])
    }

    PUT / `rolesRoot` / pathVar[UUID] >>>
      authCtx.auth ^
      roleToUpdateEntityDecoder |>> { (id: UUID, user: BackOfficeUser, roleToUpdate: RoleToUpdate) ⇒
      (for {
        _           ← EitherT(syncF.delay(requireUpdateAccess(user)))
        roleOrError ← EitherT(algebra.update(id, roleToUpdate.name, user.userName))
      } yield roleOrError.asApi).value.flatMap(handleServiceResponse[RoleToRead])
    }

    DELETE / `rolesRoot` / pathVar[UUID] >>> authCtx.auth |>> { (id: UUID, user: BackOfficeUser) ⇒
      (for {
        _              ← EitherT(syncF.delay(requireDeleteAccess(user)))
        scopeIdOrError ← EitherT(algebra.remove(id, user.userName))
      } yield scopeIdOrError).value.flatMap(handleServiceResponse[UUID])
    }
  }

}
