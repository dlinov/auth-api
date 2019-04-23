package io.dlinov.auth.domain.auth.entities

import java.time.ZonedDateTime

import io.dlinov.auth.routes.dto.PermissionKey
import io.dlinov.auth.dao.Dao.EntityId
import io.dlinov.auth.routes.dto.PermissionKey

case class Permission(
    id: EntityId,
    permissionKey: PermissionKey,
    scope: Scope,
    createdBy: String,
    updatedBy: String,
    status: Int,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime)
