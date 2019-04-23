package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

case class ScopeToRead(
    id: UUID,
    parentId: Option[UUID],
    name: String,
    description: Option[String] = None,
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime)
