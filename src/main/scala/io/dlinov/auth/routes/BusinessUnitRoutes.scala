package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.{Async, Sync}
import cats.syntax.flatMap._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.BusinessUnitAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate}
import org.http4s.AuthedRoutes
import io.dlinov.auth.routes.dto.implicits.Implicits.BusinessUnitConverter
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.json.EntityDecoders

class BusinessUnitRoutes[F[_]](
    algebra: BusinessUnitAlgebra[F]
)(implicit override protected val asyncF: Async[F])
    extends AuthenticatedRoutes[F]
    with AuthorizationBehavior
    with EntityDecoders[F] {
  import Routes._

  private val buRoot            = ApiRoot / "business_units"
  override protected val scopes = new Scopes("business_units")

  override val routes: AuthedRoutes[BackOfficeUser, F] = AuthedRoutes.of {
    case req @ POST -> `buRoot` :? MaybeReactivateQParam(maybeReactivate) as user =>
      req.req.as[BusinessUnitToCreate].flatMap { newBusinessUnit =>
        (for {
          _    <- EitherT(Sync[F].delay(requireCreateAccess(user)))
          resp <- EitherT {
            import newBusinessUnit._
            algebra.create(name, user.userName, maybeReactivate)
          }
        } yield resp.asApi).value
          .flatMap(handleServiceCreateResponse[BusinessUnitToRead])
      }

    case GET -> `buRoot` :? MaybeLimitQParam(maybeLimit) +& MaybeOffsetQParam(
          maybeOffset
        ) as user =>
      (for {
        _    <- EitherT(Sync[F].delay(requireReadAccess(user)))
        resp <- EitherT(algebra.findAll(maybeLimit, maybeOffset))
      } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
        .flatMap(handleServiceResponse[PaginatedResult[BusinessUnitToRead]])

    case GET -> `buRoot` / UUIDVar(id) as user =>
      (for {
        _         <- EitherT(Sync[F].delay(requireReadAccess(user)))
        buOrError <- EitherT(algebra.findById(id))
      } yield buOrError.asApi).value
        .flatMap(handleServiceResponse[BusinessUnitToRead])

    case req @ PUT -> `buRoot` / UUIDVar(id) as user =>
      req.req.as[BusinessUnitToUpdate].flatMap { buToUpdate =>
        (for {
          _            <- EitherT(Sync[F].delay(requireUpdateAccess(user)))
          scopeOrError <- EitherT(algebra.update(id, buToUpdate.name, user.userName))
        } yield scopeOrError.asApi).value
          .flatMap(handleServiceResponse[BusinessUnitToRead])
      }

    case DELETE -> `buRoot` / UUIDVar(id) as user =>
      (for {
        _              <- EitherT(Sync[F].delay(requireDeleteAccess(user)))
        scopeIdOrError <- EitherT(algebra.remove(id, user.userName))
      } yield scopeIdOrError).value
        .flatMap(handleServiceResponse[UUID])
  }

}
