package io.dlinov.auth.routes.dto

import java.util.UUID

import io.dlinov.auth.util.Constants

// import io.swagger.annotations.{ApiModel, ApiModelProperty}

// @ApiModel(value = "CustomerDocumentToCreate")
final case class DocumentToCreate(
    customerId: UUID,
    /*@ApiModelProperty(name = "document_type", required = true) */ documentType: String,
    /*@ApiModelProperty(name = "document_type_identifier", required = false) */ documentTypeIdentifier: Option[
      String
    ],
    /*@ApiModelProperty(name = "purpose", required = true) */ purpose: String
)

object DocumentToCreate {
  val empty = new DocumentToCreate(
    customerId = Constants.EmptyUUID,
    documentType = "",
    documentTypeIdentifier = None,
    purpose = ""
  )
}
