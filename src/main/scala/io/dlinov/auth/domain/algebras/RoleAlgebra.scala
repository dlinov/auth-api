package io.dlinov.auth.domain.algebras

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, RoleFDao}
import io.dlinov.auth.domain.{BaseService, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.domain.{BaseService, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, RoleFDao}

class RoleAlgebra(
    dao: RoleFDao,
    userDao: BackOfficeUserFDao) extends BaseService {
  import BaseService._

  def create(
    name: String,
    createdBy: String,
    maybeReactivate: Option[Boolean]): IO[ServiceResponse[Role]] = {
    val reactivate = maybeReactivate.getOrElse(false)
    for {
      roleOrError ← dao.create(name, createdBy, reactivate)
    } yield roleOrError.asServiceResponse
  }

  def findById(id: UUID): IO[ServiceResponse[Role]] = {
    for {
      maybeBusinessUnit ← dao.findById(id)
    } yield maybeBusinessUnit
      .asServiceResponse
      .flatMap(_.toRight(notFoundEntityError(s"Role id='$id' couldn't be found")))
  }

  def findAll(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): IO[ServiceResponse[PaginatedResult[Role]]] = {
    for {
      roleListOrError ← dao.findAll(maybeLimit, maybeOffset)
    } yield roleListOrError.asServiceResponse
  }

  def update(
    id: UUID,
    name: String,
    updatedBy: String): IO[ServiceResponse[Role]] = {
    for {
      maybeRoleOrError ← dao.update(id, name, updatedBy)
    } yield maybeRoleOrError
      .asServiceResponse
      .flatMap(_.toRight(notFoundEntityError(s"Role id='$id' couldn't be found")))
  }

  def remove(
    id: UUID,
    updatedBy: String): IO[ServiceResponse[UUID]] = {
    val roleIdOrError = for {
      activeUsersCount ← EitherT(userDao.countActiveByRoleId(id).map(_.asServiceResponse))
      maybeRoleOrError ← if (activeUsersCount == 0) {
        EitherT(dao.remove(id, updatedBy).map(_.asServiceResponse))
      } else {
        EitherT.leftT[IO, Option[Role]](
          validationError(s"There is still $activeUsersCount active user(s) of this role"))
      }
      roleOrError ← EitherT.fromOption[IO](
        maybeRoleOrError,
        notFoundEntityError(s"Scope id='$id' couldn't be found"))
    } yield roleOrError.id
    roleIdOrError.value
  }
}
