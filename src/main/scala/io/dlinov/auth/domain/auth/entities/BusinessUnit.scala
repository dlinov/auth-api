package io.dlinov.auth.domain.auth.entities

import java.time.ZonedDateTime
import java.util.UUID

case class BusinessUnit(
    id: UUID,
    name: String,
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime)
