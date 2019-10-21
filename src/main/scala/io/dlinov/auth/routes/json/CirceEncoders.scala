package io.dlinov.auth.routes.json

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
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
  DocumentToRead,
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
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email, PasswordResetClaim}
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto.ApiError.ErrorParams
import io.dlinov.auth.routes.dto.PermissionKeys.{
  BusinessUnitAndRolePermissionKey,
  UserPermissionKey
}
import io.dlinov.auth.routes.dto._

object CirceEncoders extends CirceConfigProvider {
  implicit override val config: Configuration = CirceConfigProvider.snakeConfig

  implicit final val errorCodeEncoder: Encoder[ErrorCode] = deriveEnumerationEncoder[ErrorCode]
  implicit final val errorParamsEncoder: Encoder.AsObject[ErrorParams] =
    deriveConfiguredEncoder[ErrorParams]
  implicit final val apiErrorEncoder: Encoder.AsObject[ApiError] = deriveConfiguredEncoder[ApiError]

  implicit final val scopeToCreateEncoder: Encoder.AsObject[ScopeToCreate] =
    deriveConfiguredEncoder[ScopeToCreate]
  implicit final val scopeToReadEncoder: Encoder.AsObject[ScopeToRead] =
    deriveConfiguredEncoder[ScopeToRead]
  implicit final val scopeToUpdateEncoder: Encoder.AsObject[ScopeToUpdate] =
    deriveConfiguredEncoder[ScopeToUpdate]

  implicit final val roleToCreateEncoder: Encoder.AsObject[RoleToCreate] =
    deriveConfiguredEncoder[RoleToCreate]
  implicit final val roleToReadEncoder: Encoder.AsObject[RoleToRead] =
    deriveConfiguredEncoder[RoleToRead]
  implicit final val roleToUpdateEncoder: Encoder.AsObject[RoleToUpdate] =
    deriveConfiguredEncoder[RoleToUpdate]

  implicit final val businessUnitToCreateEncoder: Encoder.AsObject[BusinessUnitToCreate] =
    deriveConfiguredEncoder[BusinessUnitToCreate]
  implicit final val businessUnitToReadEncoder: Encoder.AsObject[BusinessUnitToRead] =
    deriveConfiguredEncoder[BusinessUnitToRead]
  implicit final val businessUnitToUpdateEncoder: Encoder.AsObject[BusinessUnitToUpdate] =
    deriveConfiguredEncoder[BusinessUnitToUpdate]

  implicit final val burPKeyEncoder: Encoder.AsObject[BusinessUnitAndRolePermissionKey] =
    deriveConfiguredEncoder[BusinessUnitAndRolePermissionKey]
  implicit final val uPKeyEncoder: Encoder.AsObject[UserPermissionKey] =
    deriveConfiguredEncoder[UserPermissionKey]
  implicit final val pKeyEncoder: Encoder.AsObject[PermissionKey] =
    deriveConfiguredEncoder[PermissionKey].mapJsonObject(j ⇒ j(j.keys.head).get.asObject.get)
  implicit final val permissionToCreateEncoder: Encoder.AsObject[PermissionToCreate] =
    deriveConfiguredEncoder[PermissionToCreate]
  implicit final val permissionToReadEncoder: Encoder.AsObject[PermissionToRead] =
    deriveConfiguredEncoder[PermissionToRead]
  implicit final val permissionToUpdateEncoder: Encoder.AsObject[PermissionToUpdate] =
    deriveConfiguredEncoder[PermissionToUpdate]

  implicit final val backOfficeUserToCreateDecoder: Encoder.AsObject[BackOfficeUserToCreate] =
    deriveConfiguredEncoder[BackOfficeUserToCreate]
  implicit final val backOfficeUserToReadEncoder: Encoder.AsObject[BackOfficeUserToRead] =
    deriveConfiguredEncoder[BackOfficeUserToRead]
  implicit final val backOfficeUserToUpdateEncoder: Encoder.AsObject[BackOfficeUserToUpdate] =
    deriveConfiguredEncoder[BackOfficeUserToUpdate]

  implicit final val passwordResetClaimEncoder: Encoder.AsObject[PasswordResetClaim] =
    deriveConfiguredEncoder[PasswordResetClaim]
  implicit final val passwordLinkRequestEncoder: Encoder.AsObject[ResetPasswordLinkRequest] =
    deriveConfiguredEncoder[ResetPasswordLinkRequest]
  implicit final val passwordResetEncoder: Encoder.AsObject[PasswordReset] =
    deriveConfiguredEncoder[PasswordReset]

  implicit final val emailEncoder: Encoder[Email] = Encoder.encodeString.contramap[Email](_.value)
  implicit final val credentialsToReadEncoder: Encoder.AsObject[CredentialsToRead] =
    deriveConfiguredEncoder[CredentialsToRead]
  implicit final val credentialsToUpdateEncoder: Encoder.AsObject[CredentialsToUpdate] =
    deriveConfiguredEncoder[CredentialsToUpdate]
  implicit final val claimContentEncoder: Encoder.AsObject[ClaimContent] =
    deriveConfiguredEncoder[ClaimContent]
  implicit final val loginResponseEncoder: Encoder.AsObject[LoginResponse] =
    deriveConfiguredEncoder[LoginResponse]
  implicit final val loginStatusResponseEncoder: Encoder.AsObject[LoginStatusResponse] =
    deriveConfiguredEncoder[LoginStatusResponse]

  implicit final val healthInfoEncoder: Encoder.AsObject[HealthInfo] =
    deriveConfiguredEncoder[HealthInfo]

  implicit final val documentToReadEncoder: Encoder.AsObject[DocumentToRead] =
    deriveConfiguredEncoder[DocumentToRead]

  /*
    Idea is to declare page encoders for every type we need here, in the bottom of the file
    rather then use generics and generate them in runtime
   */
  implicit final val documentToReadPageEncoder: Encoder.AsObject[PaginatedResult[DocumentToRead]] =
    deriveConfiguredEncoder[PaginatedResult[DocumentToRead]]
  implicit final val bouToReadPageEncoder: Encoder.AsObject[PaginatedResult[BackOfficeUserToRead]] =
    deriveConfiguredEncoder[PaginatedResult[BackOfficeUserToRead]]
  implicit final val buToReadPageEncoder: Encoder.AsObject[PaginatedResult[BusinessUnitToRead]] =
    deriveConfiguredEncoder[PaginatedResult[BusinessUnitToRead]]
  implicit final val pToReadPageEncoder: Encoder.AsObject[PaginatedResult[PermissionToRead]] =
    deriveConfiguredEncoder[PaginatedResult[PermissionToRead]]
  implicit final val roleToReadPageEncoder: Encoder.AsObject[PaginatedResult[RoleToRead]] =
    deriveConfiguredEncoder[PaginatedResult[RoleToRead]]
  implicit final val scopeToReadPageEncoder: Encoder.AsObject[PaginatedResult[ScopeToRead]] =
    deriveConfiguredEncoder[PaginatedResult[ScopeToRead]]
}
