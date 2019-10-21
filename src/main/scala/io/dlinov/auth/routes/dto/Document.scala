package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.dlinov.auth.util.Constants

// import io.swagger.annotations.{ApiModel, ApiModelProperty}

// @ApiModel(value = "CustomerDocumentCreated")
case class Document(
    /*@ApiModelProperty(name = "document_id", required = true) */ documentId: UUID,
    /*@ApiModelProperty(name = "customer_id", required = true) */ customerId: UUID,
    /*@ApiModelProperty(name = "document_type", required = true) */ documentType: String,
    /*@ApiModelProperty(name = "document_type_identifier", required = false) */ documentTypeIdentifier: Option[
      String
    ],
    /*@ApiModelProperty(name = "purpose", required = true) */ purpose: String,
    status: String,
    rejectionReason: Option[String],
    /*@ApiModelProperty(name = "created_by", required = true) */ createdBy: String,
    /*@ApiModelProperty(name = "created_at", required = true) */ createdAt: ZonedDateTime,
    uploadedBy: Option[String],
    uploadedAt: Option[ZonedDateTime],
    checkedBy: Option[String],
    checkedAt: Option[ZonedDateTime],
    link: Option[String]
)

object Document {
  val empty = new Document(
    documentId = Constants.EmptyUUID,
    customerId = Constants.EmptyUUID,
    documentType = "",
    documentTypeIdentifier = None,
    purpose = "",
    status = "PENDING",
    rejectionReason = None,
    createdAt = Constants.MinDateTime,
    createdBy = "",
    uploadedBy = None,
    uploadedAt = None,
    checkedBy = None,
    checkedAt = None,
    link = None
  )
}
