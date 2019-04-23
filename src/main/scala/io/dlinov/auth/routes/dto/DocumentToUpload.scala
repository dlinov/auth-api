package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime

import io.dlinov.auth.util.Constants

// import io.swagger.annotations.{ApiModel, ApiModelProperty}

// @ApiModel(value = "CustomerDocumentToUpload")
case class DocumentToUpload(
    /*@ApiModelProperty(name = "file_blob", required = true) */ fileBlob: String,
    /*@ApiModelProperty(name = "file_blob_preview", required = true) */ fileBlobPreview: Option[String],
    /*@ApiModelProperty(name = "uploaded_at", required = true) */ uploadedAt: ZonedDateTime)

object DocumentToUpload {
  val empty = new DocumentToUpload(
    fileBlob = "",
    fileBlobPreview = None,
    uploadedAt = Constants.MinDateTime)
}
