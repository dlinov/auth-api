package io.dlinov.auth.routes.dto

import java.util.UUID

case class PermissionToUpdate(permissionKey: Option[PermissionKey], scopeId: Option[UUID])
