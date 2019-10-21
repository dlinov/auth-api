package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

final case class DocumentToRead(
    id: UUID,
    customerId: UUID,
    documentType: String,
    documentTypeIdentifier: Option[String],
    purpose: String,
    link: Option[String],
    createdBy: String,
    createdAt: ZonedDateTime
)
