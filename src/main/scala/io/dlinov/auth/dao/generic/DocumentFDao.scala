package io.dlinov.auth.dao.generic

import java.time.ZonedDateTime
import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.Document
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.Document

trait DocumentFDao extends Dao {

  def create(
    customerId: UUID,
    documentType: String,
    documentTypeIdentifier: Option[String],
    purpose: String,
    blobId: String,
    createdBy: String): IO[DaoResponse[Document]]

  def findById(id: UUID): IO[DaoResponse[Option[Document]]]

  def find(
    maybeCustomerId: Option[UUID],
    maybeStatus: Option[String],
    maybeStartDate: Option[ZonedDateTime],
    maybeEndDate: Option[ZonedDateTime],
    maybeOrderBy: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): IO[DaoResponse[PaginatedResult[Document]]]

  def approve(id: UUID, updatedBy: String): IO[DaoResponse[Option[Document]]]

  def reject(id: UUID, updatedBy: String): IO[DaoResponse[Option[Document]]]

}
