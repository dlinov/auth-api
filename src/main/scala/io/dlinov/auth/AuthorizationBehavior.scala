package io.dlinov.auth

import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Scopes}

trait AuthorizationBehavior {
  protected def scopes: Scopes

  def checkPermissions(scopeTypes: Scopes.Type*)(user: BackOfficeUser): Either[ServiceError, Unit] = {
    val required = scopeTypes.map(scopes.byType).toSet + scopes.parent
    if (user.permissions.exists(p ⇒ required.contains(p.scope.name))) {
      AuthorizationBehavior.success
    } else {
      Left(ServiceError.insufficientPermissionsError("Required any of " + required.mkString("{", ", ", "}")))
    }
  }

  val requireCreateAccess: BackOfficeUser ⇒ Either[ServiceError, Unit] = checkPermissions(Scopes.Create)(_)
  val requireReadAccess: BackOfficeUser ⇒ Either[ServiceError, Unit] = checkPermissions(Scopes.Detail)(_)
  val requireUpdateAccess: BackOfficeUser ⇒ Either[ServiceError, Unit] = checkPermissions(Scopes.Update)(_)
  val requireDeleteAccess: BackOfficeUser ⇒ Either[ServiceError, Unit] = checkPermissions(Scopes.Delete)(_)
  val requireFullAccess: BackOfficeUser ⇒ Either[ServiceError, Unit] = checkPermissions(Scopes.Parent)(_)
}

object AuthorizationBehavior {

  private val success = Right(())

}
