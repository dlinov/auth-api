package io.dlinov.auth.domain.algebras

import java.time.ZonedDateTime
import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.dao.generic.{BlobFDao, BlobTmpFDao, DocumentFDao}
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.routes.dto.Document
import io.dlinov.auth.dao.generic.{BlobFDao, BlobTmpFDao, DocumentFDao}
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.routes.dto._

class DocumentAlgebra(
    dao: DocumentFDao,
    blobDao: BlobFDao,
    tmpBlobDao: BlobTmpFDao)
  extends BaseService {
  import BaseService._

  def createDocument(
    customerId: UUID,
    documentType: String,
    documentTypeIdentifier: Option[String],
    purpose: String,
    fileName: String,
    bytes: Array[Byte],
    createdBy: String): IO[ServiceResponse[Document]] = {
    val expiration = tmpBlobDao.defaultDuration
    val generatedFileName = DocumentAlgebra.genBlobName(customerId, fileName)
    (for {
      blobId ← EitherT(tmpBlobDao.saveBlob(bytes, generatedFileName, expiration))
      doc ← EitherT {
        dao.create(
          customerId,
          documentType,
          documentTypeIdentifier,
          purpose,
          blobId,
          createdBy)
      }
    } yield doc).value
      .map(_.asServiceResponse)
  }

  def findById(id: UUID): IO[ServiceResponse[Document]] = {
    for {
      doc ← dao.findById(id)
    } yield doc
      .asServiceResponse
      .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Document $id couldn't be found")))
  }

  def findMany(
    maybeCustomerId: Option[UUID],
    maybeStatus: Option[String],
    maybeStartDate: Option[ZonedDateTime],
    maybeEndDate: Option[ZonedDateTime],
    maybeOrderBy: Option[String],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): IO[ServiceResponse[PaginatedResult[Document]]] = {
    for {
      docs ← dao.find(
        maybeCustomerId = maybeCustomerId,
        maybeStatus = maybeStatus,
        maybeStartDate = maybeStartDate,
        maybeEndDate = maybeEndDate,
        maybeOrderBy = maybeOrderBy,
        maybeLimit = maybeLimit,
        maybeOffset = maybeOffset)
    } yield docs.asServiceResponse
  }

  def approvePendingDocument(
    docId: UUID,
    approvedBy: String): IO[ServiceResponse[Document]] = {
    (for {
      doc ← EitherT {
        dao.approve(docId, approvedBy)
          .map(_.asServiceResponse
            .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Document $docId couldn't be found"))))
      }
      name = doc.link.getOrElse("")
      bytes ← EitherT {
        tmpBlobDao.loadBlob(name, approvedBy)
          .map(_.asServiceResponse
            .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Document blob '$name' couldn't be found"))))
      }
      moved ← EitherT {
        blobDao.saveBlob(bytes, name)
          .map(_.asServiceResponse)
      }
      // TODO: send kafka message/call endpoint
    } yield doc).value
  }

  def rejectPendingDocument(docId: UUID, rejectedBy: String): IO[ServiceResponse[Document]] = {
    (for {
      doc ← EitherT {
        dao.reject(docId, rejectedBy)
          .map(_.asServiceResponse
            .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Document $docId couldn't be found"))))
      }
      name = doc.link.getOrElse("")
      _ ← EitherT {
        tmpBlobDao.removeBlob(name)
          .map(_.asServiceResponse)
      }
    } yield doc).value
  }

  def loadBlob(docId: UUID, blobId: String, userName: String): IO[ServiceResponse[Array[Byte]]] = {
    for {
      doc ← blobDao.loadBlob(blobId, userName)
    } yield doc
      .asServiceResponse
      .flatMap(_.toRight(ServiceError.notFoundEntityError(s"Blob $blobId for doc $docId couldn't be found")))
  }

}

object DocumentAlgebra {
  private def genBlobName(customerId: UUID, fileName: String): String = s"$customerId/${UUID.randomUUID()}_$fileName"
}
