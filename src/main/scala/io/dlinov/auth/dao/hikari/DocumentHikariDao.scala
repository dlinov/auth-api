package io.dlinov.auth.dao.hikari

import java.time.ZonedDateTime
import java.util.UUID

import cats.effect.IO
import cats.syntax.either._
import doobie._
import doobie.implicits._
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.DocumentFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.Document
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.dao.generic.DocumentFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.Document

import scala.collection.mutable.ListBuffer

class DocumentHikariDao(db: DBFApi[IO])
  extends DocumentFDao {

  import HikariDBFApi._
  import DocumentHikariDao._

  override def create(
    customerId: UUID,
    documentType: String,
    documentTypeIdentifier: Option[String],
    purpose: String,
    blobId: String,
    createdBy: String): IO[DaoResponse[Document]] = {
    for {
      xa ← db.transactor
      result ← createInternal(
        customerId = customerId,
        documentType = documentType,
        documentTypeIdentifier = documentTypeIdentifier,
        purpose = purpose,
        blobId = blobId,
        createdBy = createdBy)
        .transact(xa)
        .attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .create($customerId,..,$createdBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findById(id: UUID): IO[DaoResponse[Option[Document]]] = {
    for {
      xa ← db.transactor
      maybeDoc ← findByIdInternal(id).transact(xa).attempt
    } yield maybeDoc.leftMap { exc ⇒
      val msg = s"Unexpected error in .findById($id): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def find(
    maybeCustomerId: Option[UUID],
    maybeStatus: Option[String],
    maybeStartDate: Option[ZonedDateTime],
    maybeEndDate: Option[ZonedDateTime],
    maybeOrderBy: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): IO[DaoResponse[PaginatedResult[Document]]] = {
    for {
      xa ← db.transactor
      docsOrError ← findInternal(
        maybeCustomerId = maybeCustomerId,
        maybeStatus = maybeStatus,
        maybeStartDate = maybeStartDate,
        maybeEndDate = maybeEndDate,
        maybeOrderBy = maybeOrderBy,
        maybeLimit = maybeLimit,
        maybeOffset = maybeOffset).transact(xa).attempt
    } yield docsOrError
      .map { docsAndTotal ⇒
        PaginatedResult(
          total = docsAndTotal._2,
          results = docsAndTotal._1,
          limit = maybeLimit.getOrElse(Int.MaxValue),
          offset = maybeOffset.getOrElse(0))
      }
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .find(..): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
  }

  override def approve(id: UUID, updatedBy: String): IO[DaoResponse[Option[Document]]] = {
    for {
      xa ← db.transactor
      result ← (for {
        _ ← approveQuery(id, updatedBy).update.run
        maybeDoc ← findByIdInternal(id)
      } yield maybeDoc).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .approve($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def reject(id: UUID, updatedBy: String): IO[DaoResponse[Option[Document]]] = {
    for {
      xa ← db.transactor
      result ← (for {
        _ ← rejectQuery(id, updatedBy).update.run
        maybeDoc ← findByIdInternal(id)
      } yield maybeDoc).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .reject($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  private[hikari] def createInternal(
    customerId: UUID,
    documentType: String,
    documentTypeIdentifier: Option[String],
    purpose: String,
    blobId: String,
    createdBy: String): ConnectionIO[Document] = {
    val id = UUID.randomUUID()
    for {
      _ ← insertQuery(
        id = id,
        customerId = customerId,
        docType = documentType,
        docTypeIdentifier = documentTypeIdentifier,
        purpose = purpose,
        blobId = blobId,
        cBy = createdBy,
        cAt = nowUTC).update.run
      bu ← fetchByIdInternal(id)
    } yield bu
  }

  private[hikari] def findByIdInternal(id: UUID): ConnectionIO[Option[Document]] = {
    queryById(id)
      .query[Document]
      .option
  }

  private[hikari] def fetchByIdInternal(id: UUID): ConnectionIO[Document] = {
    queryById(id)
      .query[Document]
      .unique
  }

  private[hikari] def findInternal(
    maybeCustomerId: Option[UUID],
    maybeStatus: Option[String],
    maybeStartDate: Option[ZonedDateTime],
    maybeEndDate: Option[ZonedDateTime],
    maybeOrderBy: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): ConnectionIO[(Seq[Document], Int)] = {
    val q = query(
      maybeCustomerId = maybeCustomerId,
      maybeStatus = maybeStatus,
      maybeStartDate = maybeStartDate,
      maybeEndDate = maybeEndDate,
      maybeOrderBy = maybeOrderBy,
      maybeLimit = maybeLimit,
      maybeOffset = maybeOffset)(_)
    for {
      total ← q(true).query[Int].unique
      page ← q(false).query[Document].to[Seq]
    } yield (page, total)

  }
}

object DocumentHikariDao {
  import HikariDBFApi._ // important!

  final val TableName: Fragment = Fragment.const("documents")

  private final val columns = Seq("id", "customerId", "documentType", "documentTypeIdentifier", "purpose", "status",
    "rejectionReason", "createdBy", "createdAt", "uploadedBy", "uploadedAt", "checkedBy", "checkedAt", "blobId")
  private final val columnsAsString = Fragment.const(columns.mkString(", "))

  val SelectFromTable: Fragment = Fragment.const("SELECT") ++ columnsAsString ++ Fragment.const("FROM") ++ TableName
  val SelectCountFromTable: Fragment = Fragment.const("SELECT COUNT(*) FROM") ++ TableName

  val InsertIntoTable: Fragment =
    Fragment.const("INSERT INTO") ++ TableName ++ Fragment.const("(") ++ columnsAsString ++ Fragment.const(") VALUES")

  val UpdateTable: Fragment =
    Fragment.const("UPDATE") ++ TableName ++ Fragment.const(" SET ")

  def queryById(id: UUID): Fragment = SelectFromTable ++ fr"WHERE id = $id"

  def query(
    maybeCustomerId: Option[UUID],
    maybeStatus: Option[String],
    maybeStartDate: Option[ZonedDateTime],
    maybeEndDate: Option[ZonedDateTime],
    maybeOrderBy: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int])(count: Boolean): Fragment = {
    val fragments = ListBuffer.newBuilder[Fragment]
    maybeCustomerId.foreach(cid ⇒ fragments += fr"customerId = $cid")
    maybeStatus.foreach(status ⇒ fragments += fr"status = $status")
    maybeStartDate.foreach(d ⇒ fragments += fr"createdAt >= $d")
    maybeEndDate.foreach(d ⇒ fragments += fr"createdAt <= $d")
    val baseFragment = if (count) {
      SelectCountFromTable
    } else {
      maybeOrderBy.foreach(p ⇒ fragments += fr"ORDER BY $p")
      maybeLimit.foreach(n ⇒ fragments += fr"LIMIT $n")
      maybeOffset.foreach(n ⇒ fragments += fr"OFFSET $n")
      SelectFromTable
    }
    fragments.result().foldLeft(baseFragment)(_ ++ _)
  }

  def insertQuery(
    id: UUID,
    customerId: UUID,
    docType: String,
    docTypeIdentifier: Option[String],
    purpose: String,
    blobId: String,
    cBy: String,
    cAt: ZonedDateTime): Fragment = {
    InsertIntoTable ++
      fr"($id, $customerId, $docType, $docTypeIdentifier, $purpose, 'PENDING', NULL, $cBy, $cAt," ++
      fr"NULL, NULL, NULL, NULL, $blobId)"
  }

  def reactivateQuery(
    id: UUID,
    name: String,
    cBy: String): Fragment = {
    UpdateTable ++ fr"name = $name, status = 1, uBy = $cBy WHERE id = $id;"
  }

  def approveQuery(id: UUID, approvedBy: String): Fragment = updateQuery(id, "APPROVED", approvedBy)

  def rejectQuery(id: UUID, rejectedBy: String): Fragment = updateQuery(id, "REJECTED", rejectedBy)

  private def updateQuery(
    id: UUID,
    status: String,
    uBy: String): Fragment = {
    UpdateTable ++ fr"status = $status, uBy = $uBy WHERE id = $id;"
  }
}
