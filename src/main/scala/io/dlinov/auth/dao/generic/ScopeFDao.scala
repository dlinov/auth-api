package io.dlinov.auth.dao.generic

import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Scope
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.Scope
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult

trait ScopeFDao extends Dao {
  def create(
    name: String,
    parentId: Option[UUID],
    description: Option[String],
    createdBy: String,
    reactivate: Boolean): IO[DaoResponse[Scope]]

  def findById(id: UUID): IO[DaoResponse[Option[Scope]]]

  def findAll(maybeLimit: Option[Int], maybeOffset: Option[Int]): IO[DaoResponse[PaginatedResult[Scope]]]

  def update(
    id: UUID,
    description: Option[String],
    updatedBy: String): IO[DaoResponse[Option[Scope]]]

  def remove(id: UUID, updatedBy: String): IO[DaoResponse[Option[UUID]]]
}
