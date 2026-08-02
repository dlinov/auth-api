package io.dlinov.auth.routes.proxied

import java.time.ZonedDateTime
import java.util.UUID

import io.dlinov.auth.domain.auth.entities._
import io.dlinov.auth.routes.dto.PermissionKeys.UserPermissionKey

/** Small builder for in-memory `BackOfficeUser` fixtures carrying a specific set of granted
  * permission scope names, used to unit-test proxy authorization/forwarding without needing a
  * database.
  */
object ProxyTestFixtures {
  private val now = ZonedDateTime.now()

  def scope(name: String): Scope =
    Scope(UUID.randomUUID(), None, name, None, "system", "system", now, now)

  def permission(scopeName: String): Permission =
    Permission(
      id = UUID.randomUUID(),
      permissionKey = UserPermissionKey(UUID.randomUUID()),
      scope = scope(scopeName),
      createdBy = "system",
      updatedBy = "system",
      status = 1,
      createdTime = now,
      updatedTime = now
    )

  def userWithScopes(scopeNames: String*): BackOfficeUser = userWithPermissions(
    scopeNames.map(permission)*
  )

  def userWithPermissions(permissions: Permission*): BackOfficeUser = BackOfficeUser(
    id = UUID.randomUUID(),
    userName = "test-user",
    email = Email("test-user@foo.bar"),
    phoneNumber = None,
    firstName = "Test",
    middleName = None,
    lastName = "User",
    description = None,
    homePage = None,
    activeLanguage = None,
    customData = None,
    lastLoginTimestamp = None,
    role = Role(UUID.randomUUID(), "role", "system", "system", now, now),
    businessUnit = BusinessUnit(UUID.randomUUID(), "bu", "system", "system", now, now),
    permissions = permissions,
    createdBy = "system",
    updatedBy = "system",
    createdTime = now,
    updatedTime = now
  )
}
