package io.dlinov.auth.dao.generic

import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult

trait RoleFDao extends Dao {

  def create(name: String, createdBy: String, reactivate: Boolean): IO[DaoResponse[Role]]

  def findById(id: UUID): IO[DaoResponse[Option[Role]]]

  def findAll(maybeLimit: Option[Int], maybeOffset: Option[Int]): IO[DaoResponse[PaginatedResult[Role]]]

  def update(id: UUID, name: String, updatedBy: String): IO[DaoResponse[Option[Role]]]

  def remove(id: UUID, updatedBy: String): IO[DaoResponse[Option[Role]]]

}
