package io.dlinov.auth.domain

import java.util.UUID

import io.dlinov.auth.dao.DaoError
import io.dlinov.auth.dao.DaoError.{ConstraintViolationError, EntityAlreadyExistsError, EntityNotFoundError, WrongCredentials}

case class ServiceError(
    id: UUID,
    code: ErrorCode,
    message: String,
    fieldName: Option[String] = None,
    fieldValue: Option[String] = None) {

  override def toString: String = s"$message ($id)"
}

object ServiceError {

  def duplicateEntityError(id: UUID, message: String, field: String, value: String): ServiceError =
    ServiceError(id, ErrorCodes.DuplicateEntity, message, Some(field), Some(value))

  def notFoundEntityError(id: UUID, message: String): ServiceError =
    ServiceError(id, ErrorCodes.NotFoundEntity, message)

  def notFoundEntityError(message: String): ServiceError =
    notFoundEntityError(UUID.randomUUID(), message)

  def validationError(id: UUID, message: String): ServiceError =
    ServiceError(id, ErrorCodes.ValidationFailed, message)

  def notAuthorizedError(id: UUID, message: String): ServiceError =
    ServiceError(id, ErrorCodes.NotAuthorized, message)

  def captchaRequiredError(message: String): ServiceError =
    ServiceError(UUID.randomUUID(), ErrorCodes.CaptchaRequired, message)

  def invalidCaptchaError(message: String): ServiceError =
    ServiceError(UUID.randomUUID(), ErrorCodes.InvalidCaptcha, message)

  def invalidConfigError(message: String): ServiceError =
    ServiceError(UUID.randomUUID(), ErrorCodes.InvalidConfig, message)

  def accountLockedError(message: String): ServiceError =
    ServiceError(UUID.randomUUID(), ErrorCodes.AccountTemporarilyLocked, message)

  def insufficientPermissionsError(message: String): ServiceError =
    ServiceError(UUID.randomUUID(), ErrorCodes.PermissionsInsufficient, message)

  def unknownError(id: UUID, message: String): ServiceError =
    ServiceError(id, ErrorCodes.Unknown, message)

  def fromDaoError: DaoError ⇒ ServiceError = {
    case err: EntityAlreadyExistsError ⇒ duplicateEntityError(err.id, err.message, err.field, err.value)
    case err: EntityNotFoundError ⇒ notFoundEntityError(err.id, err.message)
    case err: ConstraintViolationError ⇒ validationError(err.id, err.message)
    case err: WrongCredentials ⇒ notAuthorizedError(err.id, err.message)
    case e ⇒ unknownError(e.id, e.message)
  }
}
