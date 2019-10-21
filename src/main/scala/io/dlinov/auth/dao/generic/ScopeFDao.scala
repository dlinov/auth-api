package io.dlinov.auth.dao.generic

import java.util.UUID

import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Scope
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.Scope
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult

abstract class ScopeFDao[F[_]: cats.Monad] extends Dao {
  def create(
      name: String,
      parentId: Option[UUID],
      description: Option[String],
      createdBy: String,
      reactivate: Boolean
  ): F[DaoResponse[Scope]]

  def findById(id: UUID): F[DaoResponse[Option[Scope]]]

  def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[DaoResponse[PaginatedResult[Scope]]]

  def update(
      id: UUID,
      description: Option[String],
      updatedBy: String
  ): F[DaoResponse[Option[Scope]]]

  def remove(id: UUID, updatedBy: String): F[DaoResponse[Option[UUID]]]
}
