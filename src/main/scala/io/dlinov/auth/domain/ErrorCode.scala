package io.dlinov.auth.domain

sealed trait ErrorCode

object ErrorCodes {
  case object Unknown                  extends ErrorCode
  case object DuplicateEntity          extends ErrorCode
  case object NotFoundEntity           extends ErrorCode
  case object ValidationFailed         extends ErrorCode
  case object NotAuthorized            extends ErrorCode
  case object CaptchaRequired          extends ErrorCode
  case object InvalidCaptcha           extends ErrorCode
  case object InvalidConfig            extends ErrorCode
  case object AccountTemporarilyLocked extends ErrorCode
  case object PermissionsInsufficient  extends ErrorCode
}
