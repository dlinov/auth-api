package io.dlinov.auth.dao.generic

import java.util.UUID

import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult

trait BusinessUnitFDao[F[_]] extends Dao {

  def create(name: String, createdBy: String, reactivate: Boolean): F[DaoResponse[BusinessUnit]]

  def findById(id: UUID): F[DaoResponse[Option[BusinessUnit]]]

  def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[DaoResponse[PaginatedResult[BusinessUnit]]]

  def update(id: UUID, name: String, updatedBy: String): F[DaoResponse[Option[BusinessUnit]]]

  def remove(id: UUID, updatedBy: String): F[DaoResponse[Option[BusinessUnit]]]
}
