package io.dlinov.auth.routes.dto

import java.util.UUID

sealed trait PermissionKey

object PermissionKeys {
  final case class BusinessUnitAndRolePermissionKey(buId: UUID, roleId: UUID) extends PermissionKey
  final case class UserPermissionKey(userId: UUID)                            extends PermissionKey
}
