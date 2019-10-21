package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.dlinov.auth.util.Constants

// import io.swagger.annotations.{ApiModel, ApiModelProperty}

// @ApiModel(value = "CustomerFullDocument")
case class DocumentFull(
    /*@ApiModelProperty(name = "document_id", required = true) */ documentId: UUID,
    /*@ApiModelProperty(name = "customer_id", required = true) */ customerId: UUID,
    /*@ApiModelProperty(name = "document_type", required = true) */ documentType: String,
    /*@ApiModelProperty(name = "document_type_identifier", required = false) */ documentTypeIdentifier: Option[
      String
    ],
    /*@ApiModelProperty(name = "purpose", required = true) */ purpose: String,
    /*@ApiModelProperty(name = "created_at", required = true) */ createdAt: ZonedDateTime,
    /*@ApiModelProperty(name = "created_by", required = true) */ createdBy: String,
    /*@ApiModelProperty(name = "status", required = true) */ status: String,
    /*@ApiModelProperty(name = "reason_if_rejected", required = false) */ rejectionReason: Option[
      String
    ],
    /*@ApiModelProperty(name = "checked_at", required = true) */ checkedAt: Option[ZonedDateTime],
    /*@ApiModelProperty(name = "checked_by", required = true) */ checkedBy: Option[String],
    /*@ApiModelProperty(name = "file_blob", required = false) */ fileBlob: Option[String],
    /*@ApiModelProperty(name = "file_blob_preview", required = false) */ fileBlobPreview: Option[
      String
    ],
    /*@ApiModelProperty(name = "uploaded_at", required = false) */ uploadedAt: Option[
      ZonedDateTime
    ],
    /*@ApiModelProperty(name = "uploaded_by", required = false) */ uploadedBy: Option[String]
)

object DocumentFull {
  val empty: DocumentFull = new DocumentFull(
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
    fileBlob = None,
    fileBlobPreview = None,
    uploadedAt = None,
    uploadedBy = None
  )
}
