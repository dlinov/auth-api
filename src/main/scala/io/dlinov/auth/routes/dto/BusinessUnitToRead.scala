package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

case class BusinessUnitToRead(
    id: UUID,
    name: String,
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime
)
