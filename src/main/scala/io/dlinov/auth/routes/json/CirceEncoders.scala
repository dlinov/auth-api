package io.dlinov.auth.routes.json

import io.circe.Encoder
import io.circe.derivation.{Configuration, ConfiguredEncoder}
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.domain.ErrorCodes._
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email, PasswordResetClaim}
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto.{
  ApiError,
  BackOfficeUserToCreate,
  BackOfficeUserToRead,
  BackOfficeUserToUpdate,
  BusinessUnitToCreate,
  BusinessUnitToRead,
  BusinessUnitToUpdate,
  CredentialsToRead,
  CredentialsToUpdate,
  LoginResponse,
  LoginStatusResponse,
  PasswordReset,
  PermissionKey,
  PermissionToCreate,
  PermissionToRead,
  PermissionToUpdate,
  ResetPasswordLinkRequest,
  RoleToCreate,
  RoleToRead,
  RoleToUpdate,
  ScopeToCreate,
  ScopeToRead,
  ScopeToUpdate
}
import io.dlinov.auth.routes.dto.ApiError.ErrorParams
import io.dlinov.auth.routes.dto.PermissionKeys.{
  BusinessUnitAndRolePermissionKey,
  UserPermissionKey
}

object CirceEncoders extends CirceConfigProvider {
  implicit override val config: Configuration = CirceConfigProvider.snakeConfig

  // circe-generic-extras' deriveEnumerationEncoder has no Scala 3 / circe 0.14 core equivalent;
  // ErrorCode is a plain enumeration of case objects, so encode/decode it as a snake_case string.
  implicit final val errorCodeEncoder: Encoder[ErrorCode] = Encoder.encodeString.contramap {
    case Unknown                  => "unknown"
    case DuplicateEntity          => "duplicate_entity"
    case NotFoundEntity           => "not_found_entity"
    case ValidationFailed         => "validation_failed"
    case NotAuthorized            => "not_authorized"
    case CaptchaRequired          => "captcha_required"
    case InvalidCaptcha           => "invalid_captcha"
    case InvalidConfig            => "invalid_config"
    case AccountTemporarilyLocked => "account_temporarily_locked"
    case PermissionsInsufficient  => "permissions_insufficient"
  }
  implicit final val errorParamsEncoder: Encoder.AsObject[ErrorParams] =
    ConfiguredEncoder.derived[ErrorParams]
  implicit final val apiErrorEncoder: Encoder.AsObject[ApiError] =
    ConfiguredEncoder.derived[ApiError]

  implicit final val scopeToCreateEncoder: Encoder.AsObject[ScopeToCreate] =
    ConfiguredEncoder.derived[ScopeToCreate]
  implicit final val scopeToReadEncoder: Encoder.AsObject[ScopeToRead] =
    ConfiguredEncoder.derived[ScopeToRead]
  implicit final val scopeToUpdateEncoder: Encoder.AsObject[ScopeToUpdate] =
    ConfiguredEncoder.derived[ScopeToUpdate]

  implicit final val roleToCreateEncoder: Encoder.AsObject[RoleToCreate] =
    ConfiguredEncoder.derived[RoleToCreate]
  implicit final val roleToReadEncoder: Encoder.AsObject[RoleToRead] =
    ConfiguredEncoder.derived[RoleToRead]
  implicit final val roleToUpdateEncoder: Encoder.AsObject[RoleToUpdate] =
    ConfiguredEncoder.derived[RoleToUpdate]

  implicit final val businessUnitToCreateEncoder: Encoder.AsObject[BusinessUnitToCreate] =
    ConfiguredEncoder.derived[BusinessUnitToCreate]
  implicit final val businessUnitToReadEncoder: Encoder.AsObject[BusinessUnitToRead] =
    ConfiguredEncoder.derived[BusinessUnitToRead]
  implicit final val businessUnitToUpdateEncoder: Encoder.AsObject[BusinessUnitToUpdate] =
    ConfiguredEncoder.derived[BusinessUnitToUpdate]

  implicit final val burPKeyEncoder: Encoder.AsObject[BusinessUnitAndRolePermissionKey] =
    ConfiguredEncoder.derived[BusinessUnitAndRolePermissionKey]
  implicit final val uPKeyEncoder: Encoder.AsObject[UserPermissionKey] =
    ConfiguredEncoder.derived[UserPermissionKey]
  implicit final val pKeyEncoder: Encoder.AsObject[PermissionKey] =
    ConfiguredEncoder.derived[PermissionKey].mapJsonObject(j => j(j.keys.head).get.asObject.get)
  implicit final val permissionToCreateEncoder: Encoder.AsObject[PermissionToCreate] =
    ConfiguredEncoder.derived[PermissionToCreate]
  implicit final val permissionToReadEncoder: Encoder.AsObject[PermissionToRead] =
    ConfiguredEncoder.derived[PermissionToRead]
  implicit final val permissionToUpdateEncoder: Encoder.AsObject[PermissionToUpdate] =
    ConfiguredEncoder.derived[PermissionToUpdate]

  implicit final val backOfficeUserToCreateDecoder: Encoder.AsObject[BackOfficeUserToCreate] =
    ConfiguredEncoder.derived[BackOfficeUserToCreate]
  implicit final val backOfficeUserToReadEncoder: Encoder.AsObject[BackOfficeUserToRead] =
    ConfiguredEncoder.derived[BackOfficeUserToRead]
  implicit final val backOfficeUserToUpdateEncoder: Encoder.AsObject[BackOfficeUserToUpdate] =
    ConfiguredEncoder.derived[BackOfficeUserToUpdate]

  implicit final val passwordResetClaimEncoder: Encoder.AsObject[PasswordResetClaim] =
    ConfiguredEncoder.derived[PasswordResetClaim]
  implicit final val passwordLinkRequestEncoder: Encoder.AsObject[ResetPasswordLinkRequest] =
    ConfiguredEncoder.derived[ResetPasswordLinkRequest]
  implicit final val passwordResetEncoder: Encoder.AsObject[PasswordReset] =
    ConfiguredEncoder.derived[PasswordReset]

  implicit final val emailEncoder: Encoder[Email] = Encoder.encodeString.contramap[Email](_.value)
  implicit final val credentialsToReadEncoder: Encoder.AsObject[CredentialsToRead] =
    ConfiguredEncoder.derived[CredentialsToRead]
  implicit final val credentialsToUpdateEncoder: Encoder.AsObject[CredentialsToUpdate] =
    ConfiguredEncoder.derived[CredentialsToUpdate]
  implicit final val claimContentEncoder: Encoder.AsObject[ClaimContent] =
    ConfiguredEncoder.derived[ClaimContent]
  implicit final val loginResponseEncoder: Encoder.AsObject[LoginResponse] =
    ConfiguredEncoder.derived[LoginResponse]
  implicit final val loginStatusResponseEncoder: Encoder.AsObject[LoginStatusResponse] =
    ConfiguredEncoder.derived[LoginStatusResponse]

  implicit final val healthInfoEncoder: Encoder.AsObject[HealthInfo] =
    ConfiguredEncoder.derived[HealthInfo]

  /*
    Idea is to declare page encoders for every type we need here, in the bottom of the file
    rather then use generics and generate them in runtime
   */
  implicit final val bouToReadPageEncoder: Encoder.AsObject[PaginatedResult[BackOfficeUserToRead]] =
    ConfiguredEncoder.derived[PaginatedResult[BackOfficeUserToRead]]
  implicit final val buToReadPageEncoder: Encoder.AsObject[PaginatedResult[BusinessUnitToRead]] =
    ConfiguredEncoder.derived[PaginatedResult[BusinessUnitToRead]]
  implicit final val pToReadPageEncoder: Encoder.AsObject[PaginatedResult[PermissionToRead]] =
    ConfiguredEncoder.derived[PaginatedResult[PermissionToRead]]
  implicit final val roleToReadPageEncoder: Encoder.AsObject[PaginatedResult[RoleToRead]] =
    ConfiguredEncoder.derived[PaginatedResult[RoleToRead]]
  implicit final val scopeToReadPageEncoder: Encoder.AsObject[PaginatedResult[ScopeToRead]] =
    ConfiguredEncoder.derived[PaginatedResult[ScopeToRead]]
}
