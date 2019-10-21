package io.dlinov.auth.domain.algebras

import java.util.UUID

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, RoleFDao}
import io.dlinov.auth.domain.{BaseService, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.domain.{BaseService, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, RoleFDao}

class RoleAlgebra[F[_]: Monad](dao: RoleFDao[F], userDao: BackOfficeUserFDao[F])
    extends BaseService {
  import BaseService._

  def create(
      name: String,
      createdBy: String,
      maybeReactivate: Option[Boolean]
  ): F[ServiceResponse[Role]] = {
    val reactivate = maybeReactivate.getOrElse(false)
    for {
      roleOrError ← dao.create(name, createdBy, reactivate)
    } yield roleOrError.asServiceResponse
  }

  def findById(id: UUID): F[ServiceResponse[Role]] = {
    for {
      maybeBusinessUnit ← dao.findById(id)
    } yield maybeBusinessUnit.asServiceResponse
      .flatMap(_.toRight(notFoundEntityError(s"Role id='$id' couldn't be found")))
  }

  def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[ServiceResponse[PaginatedResult[Role]]] = {
    for {
      roleListOrError ← dao.findAll(maybeLimit, maybeOffset)
    } yield roleListOrError.asServiceResponse
  }

  def update(id: UUID, name: String, updatedBy: String): F[ServiceResponse[Role]] = {
    for {
      maybeRoleOrError ← dao.update(id, name, updatedBy)
    } yield maybeRoleOrError.asServiceResponse
      .flatMap(_.toRight(notFoundEntityError(s"Role id='$id' couldn't be found")))
  }

  def remove(id: UUID, updatedBy: String): F[ServiceResponse[UUID]] = {
    val roleIdOrError = for {
      activeUsersCount ← EitherT(userDao.countActiveByRoleId(id).map(_.asServiceResponse))
      maybeRoleOrError ← if (activeUsersCount == 0) {
        EitherT(dao.remove(id, updatedBy).map(_.asServiceResponse))
      } else {
        EitherT.leftT[F, Option[Role]](
          validationError(s"There is still $activeUsersCount active user(s) of this role")
        )
      }
      roleOrError ← EitherT
        .fromOption[F](maybeRoleOrError, notFoundEntityError(s"Scope id='$id' couldn't be found"))
    } yield roleOrError.id
    roleIdOrError.value
  }
}
