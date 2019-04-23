package io.dlinov.auth.routes.dto

import java.util.UUID

case class PermissionToCreate(
    permissionKey: PermissionKey,
    revoke: Option[Boolean],
    scopeId: UUID)
