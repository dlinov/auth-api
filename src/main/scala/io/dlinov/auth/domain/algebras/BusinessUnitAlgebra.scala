package io.dlinov.auth.domain.algebras

import java.util.UUID

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, BusinessUnitFDao}
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, BusinessUnitFDao}

class BusinessUnitAlgebra[F[_]: Monad](dao: BusinessUnitFDao[F], userDao: BackOfficeUserFDao[F])
    extends BaseService {
  import BaseService._

  def create(
      name: String,
      createdBy: String,
      maybeReactivate: Option[Boolean]
  ): F[ServiceResponse[BusinessUnit]] = {
    val reactivate = maybeReactivate.getOrElse(false)
    for {
      buOrError ← dao.create(name, createdBy, reactivate)
    } yield buOrError.asServiceResponse
  }

  def findById(id: UUID): F[ServiceResponse[BusinessUnit]] = {
    for {
      maybeBusinessUnit ← dao.findById(id)
    } yield maybeBusinessUnit.asServiceResponse
      .flatMap(
        _.toRight(ServiceError.notFoundEntityError(s"Business unit id='$id' couldn't be found"))
      )
  }

  def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[ServiceResponse[PaginatedResult[BusinessUnit]]] = {
    for {
      buListOrError ← dao.findAll(maybeLimit, maybeOffset)
    } yield buListOrError.asServiceResponse
  }

  def update(id: UUID, name: String, updatedBy: String): F[ServiceResponse[BusinessUnit]] = {
    for {
      maybeBusinessUnitOrError ← dao.update(id, name, updatedBy)
    } yield maybeBusinessUnitOrError.asServiceResponse
      .flatMap(
        _.toRight(ServiceError.notFoundEntityError(s"Business unit id='$id' couldn't be found"))
      )
  }

  def remove(id: UUID, updatedBy: String): F[ServiceResponse[UUID]] = {
    val buIdOrError = for {
      activeUsersCount ← EitherT(userDao.countActiveByBusinessUnitId(id).map(_.asServiceResponse))
      maybeBusinessUnitOrError ← if (activeUsersCount == 0) {
        EitherT(dao.remove(id, updatedBy).map(_.asServiceResponse))
      } else {
        EitherT.leftT[F, Option[BusinessUnit]] {
          validationError(s"There is still $activeUsersCount active user(s) of this business unit")
        }
      }
      buOrError ← EitherT.fromOption[F](
        maybeBusinessUnitOrError,
        notFoundEntityError(s"Scope id='$id' couldn't be found")
      )
    } yield buOrError.id
    buIdOrError.value
  }
}
