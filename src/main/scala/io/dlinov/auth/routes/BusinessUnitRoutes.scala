package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect._
import io.dlinov.auth.AuthorizationBehavior
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.algebras.BusinessUnitAlgebra
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.routes.dto.{BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate}
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.AuthorizationBehavior
import org.http4s.rho.{AuthedContext, RhoRoutes}
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.implicits.Implicits.BusinessUnitConverter
import io.dlinov.auth.domain.algebras.BusinessUnitAlgebra
import io.dlinov.auth.routes.json.EntityDecoders._
import io.dlinov.auth.routes.json.CirceEncoders._
import io.dlinov.auth.routes.dto.{BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate}

class BusinessUnitRoutes(override val authCtx: AuthedContext[IO, BackOfficeUser], algebra: BusinessUnitAlgebra)
  extends AuthenticatedRoutes[IO] with AuthorizationBehavior {
  import Routes._

  private val buRoot: String = ApiPrefix + "/business_units"
  override protected val scopes = new Scopes("business_units")

  override val routes: RhoRoutes[IO] = new RhoRoutes[IO] {
    POST / `buRoot` +?
      RhoExt.reactivateParam >>>
      authCtx.auth ^
      buToCreateEntityDecoder |>> {
        (maybeReactivate: Option[Boolean], user: BackOfficeUser, newBusinessUnit: BusinessUnitToCreate) ⇒
          (for {
            _ ← EitherT(IO(requireCreateAccess(user)))
            resp ← EitherT {
              import newBusinessUnit._
              algebra.create(name, user.userName, maybeReactivate)
            }
          } yield resp.asApi).value
            .flatMap(handleServiceCreateResponse[BusinessUnitToRead])
      }

    GET / `buRoot` +?
      RhoExt.limitAndOffsetParams >>>
      authCtx.auth |>> {
        (maybeLimit: Option[Int], maybeOffset: Option[Int], user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireReadAccess(user)))
            resp ← EitherT(algebra.findAll(maybeLimit, maybeOffset))
          } yield PaginatedResult(resp.total, resp.results.map(_.asApi), resp.limit, resp.offset)).value
            .flatMap(handleServiceResponse[PaginatedResult[BusinessUnitToRead]])
      }

    GET / `buRoot` / pathVar[UUID] >>>
      authCtx.auth |>> {
        (id: UUID, user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireReadAccess(user)))
            buOrError ← EitherT(algebra.findById(id))
          } yield buOrError.asApi).value
            .flatMap(handleServiceResponse[BusinessUnitToRead])
      }

    PUT / `buRoot` / pathVar[UUID] >>>
      authCtx.auth ^
      buToUpdateEntityDecoder |>> {
        (id: UUID, user: BackOfficeUser, buToUpdate: BusinessUnitToUpdate) ⇒
          (for {
            _ ← EitherT(IO(requireUpdateAccess(user)))
            scopeOrError ← EitherT(algebra.update(id, buToUpdate.name, user.userName))
          } yield scopeOrError.asApi).value
            .flatMap(handleServiceResponse[BusinessUnitToRead])
      }

    DELETE / `buRoot` / pathVar[UUID] >>>
      authCtx.auth |>> {
        (id: UUID, user: BackOfficeUser) ⇒
          (for {
            _ ← EitherT(IO(requireDeleteAccess(user)))
            scopeIdOrError ← EitherT(algebra.remove(id, user.userName))
          } yield scopeIdOrError).value
            .flatMap(handleServiceResponse[UUID])
      }
  }

}
