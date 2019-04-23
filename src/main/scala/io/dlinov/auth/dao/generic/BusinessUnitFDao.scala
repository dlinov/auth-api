package io.dlinov.auth.dao.generic

import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult

trait BusinessUnitFDao extends Dao {

  def create(name: String, createdBy: String, reactivate: Boolean): IO[DaoResponse[BusinessUnit]]

  def findById(id: UUID): IO[DaoResponse[Option[BusinessUnit]]]

  def findAll(maybeLimit: Option[Int], maybeOffset: Option[Int]): IO[DaoResponse[PaginatedResult[BusinessUnit]]]

  def update(id: UUID, name: String, updatedBy: String): IO[DaoResponse[Option[BusinessUnit]]]

  def remove(id: UUID, updatedBy: String): IO[DaoResponse[Option[BusinessUnit]]]
}
