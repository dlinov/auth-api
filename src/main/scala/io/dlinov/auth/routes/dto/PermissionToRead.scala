package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

case class PermissionToRead(
    id: UUID,
    scope: ScopeToRead,
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime
)
