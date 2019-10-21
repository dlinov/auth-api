package io.dlinov.auth.domain.algebras

import java.util.UUID

import cats.Monad
import cats.syntax.functor._
import io.dlinov.auth.dao.generic.ScopeFDao
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.domain.auth.entities.Scope
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.domain.BaseService._
import io.dlinov.auth.dao.generic.ScopeFDao

class ScopeAlgebra[F[_]: Monad](scopeDao: ScopeFDao[F]) extends BaseService {

  def create(
      name: String,
      parentId: Option[UUID],
      description: Option[String],
      createdBy: String,
      maybeReactivate: Option[Boolean]
  ): F[ServiceResponse[Scope]] = {
    val reactivate = maybeReactivate.getOrElse(false)
    for {
      scopeOrError ← scopeDao.create(name, parentId, description, createdBy, reactivate)
    } yield scopeOrError.asServiceResponse
  }

  def findById(id: UUID): F[ServiceResponse[Scope]] = {
    for {
      maybeScope ← scopeDao.findById(id)
    } yield maybeScope.asServiceResponse
      .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Scope id='$id' couldn't be found")))
  }

  def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[ServiceResponse[PaginatedResult[Scope]]] = {
    for {
      scopesOrError ← scopeDao.findAll(maybeLimit, maybeOffset)
    } yield scopesOrError.asServiceResponse
  }

  def update(
      id: UUID,
      description: Option[String],
      updatedBy: String
  ): F[ServiceResponse[Scope]] = {
    for {
      scopeOrError ← scopeDao.update(id, description, updatedBy)
    } yield scopeOrError.asServiceResponse
      .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Scope id='$id' couldn't be found")))
  }

  def remove(id: UUID, uBy: String): F[ServiceResponse[UUID]] = {
    for {
      maybeScope ← scopeDao.remove(id, uBy)
    } yield maybeScope.asServiceResponse
      .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Scope id='$id' couldn't be found")))
  }
}
