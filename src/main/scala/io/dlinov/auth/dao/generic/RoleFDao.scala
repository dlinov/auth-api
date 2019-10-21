package io.dlinov.auth.dao.generic

import java.util.UUID

import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult

trait RoleFDao[F[_]] extends Dao {

  def create(name: String, createdBy: String, reactivate: Boolean): F[DaoResponse[Role]]

  def findById(id: UUID): F[DaoResponse[Option[Role]]]

  def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[DaoResponse[PaginatedResult[Role]]]

  def update(id: UUID, name: String, updatedBy: String): F[DaoResponse[Option[Role]]]

  def remove(id: UUID, updatedBy: String): F[DaoResponse[Option[Role]]]

}
