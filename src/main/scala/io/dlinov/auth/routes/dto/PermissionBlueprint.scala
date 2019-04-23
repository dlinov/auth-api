package io.dlinov.auth.routes.dto

import java.util.UUID

final case class PermissionBlueprint(
    revoke: Boolean,
    pKey: PermissionKey,
    scopeId: UUID,
    createdBy: String)
