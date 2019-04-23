package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.dlinov.auth.util.Constants

// import io.swagger.annotations.{ApiModel, ApiModelProperty}

// @ApiModel(value = "CustomerSmallDocument")
case class DocumentSmall(
    /*@ApiModelProperty(name = "document_id", required = true) */ documentId: UUID,
    /*@ApiModelProperty(name = "customer_id", required = true) */ customerId: UUID,
    /*@ApiModelProperty(name = "document_type", required = true) */ documentType: String,
    /*@ApiModelProperty(name = "document_type_identifier", required = false) */ documentTypeIdentifier: Option[String],
    /*@ApiModelProperty(name = "purpose", required = true) */ purpose: String,
    /*@ApiModelProperty(name = "created_at", required = true) */ createdAt: ZonedDateTime,
    /*@ApiModelProperty(name = "created_by", required = true) */ createdBy: String,
    /*@ApiModelProperty(name = "status", required = true) */ status: String,
    /*@ApiModelProperty(name = "reason_if_rejected", required = false) */ rejectionReason: Option[String],
    /*@ApiModelProperty(name = "checked_at", required = false) */ checkedAt: Option[ZonedDateTime],
    /*@ApiModelProperty(name = "checked_by", required = false) */ checkedBy: Option[String],
    /*@ApiModelProperty(name = "file_blob_preview", required = false) */ fileBlobPreview: Option[String])

object DocumentSmall {
  val empty = new DocumentSmall(
    documentId = Constants.EmptyUUID,
    customerId = Constants.EmptyUUID,
    documentType = "",
    documentTypeIdentifier = None,
    purpose = "",
    createdAt = Constants.MinDateTime,
    createdBy = "",
    status = "",
    rejectionReason = None,
    checkedAt = None,
    checkedBy = None,
    fileBlobPreview = None)
}
