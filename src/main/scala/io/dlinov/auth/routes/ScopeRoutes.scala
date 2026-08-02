package io.dlinov.auth.routes

import java.util.UUID

import cats.Applicative
import cats.data.EitherT
import cats.effect.Async
import cats.syntax.flatMap._
import io.dlinov.auth.routes.json.EntityEncoders
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.ScopeAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{ScopeToCreate, ScopeToRead, ScopeToUpdate}
import org.http4s.AuthedRoutes
import io.dlinov.auth.routes.dto.implicits.Implicits.ScopeConverter
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders

class ScopeRoutes[F[_]](
    algebra: ScopeAlgebra[F]
)(implicit
    override protected val asyncF: Async[F]
) extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F]
    with EntityEncoders[F] {
  import Routes._

  private val scopesRoot                                       = ApiRoot / "scopes"
  override protected val scopes                                = new Scopes("scopes")
  implicit override protected def applicativeF: Applicative[F] = asyncF

  override val routes: AuthedRoutes[BackOfficeUser, F] = AuthedRoutes.of {
    case req @ POST -> `scopesRoot` :? MaybeReactivateQParam(maybeReactivate) as user =>
      req.req.as[ScopeToCreate].flatMap { newScope =>
        (for {
          _    <- EitherT(asyncF.delay(requireCreateAccess(user)))
          resp <- EitherT {
            import newScope._
            algebra.create(name, parentId, description, user.userName, maybeReactivate)
          }
        } yield resp.asApi).value.flatMap(handleServiceCreateResponse[ScopeToRead])
      }

    case GET -> `scopesRoot` :? MaybeLimitQParam(maybeLimit) +& MaybeOffsetQParam(
          maybeOffset
        ) as user =>
      (for {
        _    <- EitherT(asyncF.delay(requireReadAccess(user)))
        resp <- EitherT(algebra.findAll(maybeLimit, maybeOffset))
      } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
        .flatMap(handleServiceResponse[PaginatedResult[ScopeToRead]])

    case GET -> `scopesRoot` / UUIDVar(id) as user =>
      (for {
        _            <- EitherT(asyncF.delay(requireReadAccess(user)))
        scopeOrError <- EitherT(algebra.findById(id))
      } yield scopeOrError.asApi).value.flatMap(handleServiceResponse[ScopeToRead])

    case req @ PUT -> `scopesRoot` / UUIDVar(id) as user =>
      req.req.as[ScopeToUpdate].flatMap { scopeToUpdate =>
        (for {
          _            <- EitherT(asyncF.delay(requireUpdateAccess(user)))
          scopeOrError <- EitherT(algebra.update(id, scopeToUpdate.description, user.userName))
        } yield scopeOrError.asApi).value.flatMap(handleServiceResponse[ScopeToRead])
      }

    case DELETE -> `scopesRoot` / UUIDVar(id) as user =>
      (for {
        _              <- EitherT(asyncF.delay(requireDeleteAccess(user)))
        scopeIdOrError <- EitherT(algebra.remove(id, user.userName))
      } yield scopeIdOrError).value.flatMap(handleServiceResponse[UUID])
  }
}
