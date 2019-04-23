package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.dlinov.auth.util.Constants

// import io.swagger.annotations.{ApiModel, ApiModelProperty}

// @ApiModel(value = "CustomerDocumentUploaded")
case class DocumentEntry(
    /*@ApiModelProperty(name = "document_id", required = true) */ documentId: UUID,
    /*@ApiModelProperty(name = "encrypted_file_blob", required = true) */ encryptedFileBlob: String,
    /*@ApiModelProperty(name = "uploaded_at", required = true) */ uploadedAt: ZonedDateTime,
    /*@ApiModelProperty(name = "uploaded_by", required = true) */ uploadedBy: String)

object DocumentEntry {
  val empty = new DocumentEntry(
    documentId = Constants.EmptyUUID,
    encryptedFileBlob = "",
    uploadedAt = Constants.MinDateTime,
    uploadedBy = "")
}
