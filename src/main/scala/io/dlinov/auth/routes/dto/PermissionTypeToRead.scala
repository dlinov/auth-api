package io.dlinov.auth.routes.dto

sealed trait PermissionTypeToRead

object PermissionTypeToRead {
  case object BusinessUnit extends PermissionTypeToRead

  case object BackOfficeUser extends PermissionTypeToRead

  case object Role extends PermissionTypeToRead
}
