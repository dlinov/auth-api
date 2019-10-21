package io.dlinov.auth.routes

import java.util.UUID

import cats.Applicative
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.flatMap._
import io.dlinov.auth.routes.json.EntityEncoders
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.ScopeAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{ScopeToCreate, ScopeToRead, ScopeToUpdate}
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.algebras.ScopeAlgebra
import org.http4s.rho.{AuthedContext, RhoRoutes}
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.{ScopeToCreate, ScopeToRead, ScopeToUpdate}
import io.dlinov.auth.routes.dto.implicits.Implicits.ScopeConverter
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders

class ScopeRoutes[F[_]](
    override val authCtx: AuthedContext[F, BackOfficeUser],
    algebra: ScopeAlgebra[F]
)(
    implicit override protected val syncF: Sync[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F]
    with EntityEncoders[F] {
  import Routes._

  private val scopesRoot: String                               = ApiPrefix + "/scopes"
  override protected val scopes                                = new Scopes("scopes")
  implicit override protected def applicativeF: Applicative[F] = syncF

  override val routes: RhoRoutes[F] = new RhoRoutes[F] {
    POST / `scopesRoot` +?
      rhoExt.reactivateParam >>>
      authCtx.auth ^
      scopeToCreateEntityDecoder |>> {
      (maybeReactivate: Option[Boolean], user: BackOfficeUser, newScope: ScopeToCreate) ⇒
        (for {
          _ ← EitherT(syncF.delay(requireCreateAccess(user)))
          resp ← EitherT {
            import newScope._
            algebra.create(name, parentId, description, user.userName, maybeReactivate)
          }
        } yield resp.asApi).value.flatMap(handleServiceCreateResponse[ScopeToRead])
    }

    GET / `scopesRoot` +?
      rhoExt.limitAndOffsetParams >>>
      authCtx.auth |>> { (maybeLimit: Option[Int], maybeOffset: Option[Int], user: BackOfficeUser) ⇒
      (for {
        _    ← EitherT(syncF.delay(requireReadAccess(user)))
        resp ← EitherT(algebra.findAll(maybeLimit, maybeOffset))
      } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
        .flatMap(handleServiceResponse[PaginatedResult[ScopeToRead]])
    }

    GET / `scopesRoot` / pathVar[UUID] >>>
      authCtx.auth |>> { (id: UUID, user: BackOfficeUser) ⇒
      (for {
        _            ← EitherT(syncF.delay(requireReadAccess(user)))
        scopeOrError ← EitherT(algebra.findById(id))
      } yield scopeOrError.asApi).value.flatMap(handleServiceResponse[ScopeToRead])
    }

    PUT / `scopesRoot` / pathVar[UUID] >>>
      authCtx.auth ^
      scopeToUpdateEntityDecoder |>> {
      (id: UUID, user: BackOfficeUser, scopeToUpdate: ScopeToUpdate) ⇒
        (for {
          _            ← EitherT(syncF.delay(requireUpdateAccess(user)))
          scopeOrError ← EitherT(algebra.update(id, scopeToUpdate.description, user.userName))
        } yield scopeOrError.asApi).value.flatMap(handleServiceResponse[ScopeToRead])
    }

    DELETE / `scopesRoot` / pathVar[UUID] >>>
      authCtx.auth |>> { (id: UUID, user: BackOfficeUser) ⇒
      (for {
        _              ← EitherT(syncF.delay(requireDeleteAccess(user)))
        scopeIdOrError ← EitherT(algebra.remove(id, user.userName))
      } yield scopeIdOrError).value.flatMap(handleServiceResponse[UUID])
    }
  }
}
