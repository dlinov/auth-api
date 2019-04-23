package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.dlinov.auth.util.Constants

// import io.swagger.annotations.{ApiModel, ApiModelProperty}

// @ApiModel(value = "CustomerApprovedDocument")
case class ApprovedDocument(
    /*@ApiModelProperty(name = "document_id", required = true) */ documentId: UUID,
    /*@ApiModelProperty(name = "customer_id", required = true) */ customerId: UUID,
    /*@ApiModelProperty(name = "document_type", required = true) */ documentType: String,
    /*@ApiModelProperty(name = "document_type_identifier", required = false) */ documentTypeIdentifier: Option[String],
    /*@ApiModelProperty(name = "purpose", required = true) */ purpose: String,
    /*@ApiModelProperty(name = "created_at", required = true) */ createdAt: ZonedDateTime,
    /*@ApiModelProperty(name = "created_by", required = true) */ createdBy: String,
    /*@ApiModelProperty(name = "approved_at", required = true) */ approvedAt: ZonedDateTime,
    /*@ApiModelProperty(name = "approved_by", required = true) */ approvedBy: String)

object ApprovedDocument {
  val empty = new ApprovedDocument(
    documentId = Constants.EmptyUUID,
    customerId = Constants.EmptyUUID,
    documentType = "",
    documentTypeIdentifier = None,
    purpose = "",
    createdAt = Constants.MinDateTime,
    createdBy = "",
    approvedAt = Constants.MinDateTime,
    approvedBy = "")
}
