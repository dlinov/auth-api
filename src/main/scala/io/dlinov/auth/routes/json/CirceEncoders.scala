package io.dlinov.auth.routes.json

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email, PasswordResetClaim}
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto.{ApiError, BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate, BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate, CredentialsToRead, CredentialsToUpdate, DocumentToRead, LoginResponse, LoginStatusResponse, PasswordReset, PermissionKey, PermissionToCreate, PermissionToRead, PermissionToUpdate, ResetPasswordLinkRequest, RoleToCreate, RoleToRead, RoleToUpdate, ScopeToCreate, ScopeToRead, ScopeToUpdate}
import io.dlinov.auth.routes.dto.ApiError.ErrorParams
import io.dlinov.auth.routes.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email, PasswordResetClaim}
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto.ApiError.ErrorParams
import io.dlinov.auth.routes.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import io.dlinov.auth.routes.dto._

object CirceEncoders extends CirceConfigProvider {
  override implicit val config: Configuration = CirceConfigProvider.snakeConfig

  implicit final val errorCodeEncoder: Encoder[ErrorCode] = deriveEnumerationEncoder[ErrorCode]
  implicit final val errorParamsEncoder: Encoder.AsObject[ErrorParams] = deriveEncoder[ErrorParams]
  implicit final val apiErrorEncoder: Encoder.AsObject[ApiError] = deriveEncoder[ApiError]

  implicit final val scopeToCreateEncoder: Encoder.AsObject[ScopeToCreate] = deriveEncoder[ScopeToCreate]
  implicit final val scopeToReadEncoder: Encoder.AsObject[ScopeToRead] = deriveEncoder[ScopeToRead]
  implicit final val scopeToUpdateEncoder: Encoder.AsObject[ScopeToUpdate] = deriveEncoder[ScopeToUpdate]

  implicit final val roleToCreateEncoder: Encoder.AsObject[RoleToCreate] = deriveEncoder[RoleToCreate]
  implicit final val roleToReadEncoder: Encoder.AsObject[RoleToRead] = deriveEncoder[RoleToRead]
  implicit final val roleToUpdateEncoder: Encoder.AsObject[RoleToUpdate] = deriveEncoder[RoleToUpdate]

  implicit final val businessUnitToCreateEncoder: Encoder.AsObject[BusinessUnitToCreate] =
    deriveEncoder[BusinessUnitToCreate]
  implicit final val businessUnitToReadEncoder: Encoder.AsObject[BusinessUnitToRead] =
    deriveEncoder[BusinessUnitToRead]
  implicit final val businessUnitToUpdateEncoder: Encoder.AsObject[BusinessUnitToUpdate] =
    deriveEncoder[BusinessUnitToUpdate]

  implicit final val burPKeyEncoder: Encoder.AsObject[BusinessUnitAndRolePermissionKey] =
    deriveEncoder[BusinessUnitAndRolePermissionKey]
  implicit final val uPKeyEncoder: Encoder.AsObject[UserPermissionKey] =
    deriveEncoder[UserPermissionKey]
  implicit final val pKeyEncoder: Encoder.AsObject[PermissionKey] =
    deriveEncoder[PermissionKey].mapJsonObject(j ⇒ j(j.keys.head).get.asObject.get)
  implicit final val permissionToCreateEncoder: Encoder.AsObject[PermissionToCreate] = deriveEncoder[PermissionToCreate]
  implicit final val permissionToReadEncoder: Encoder.AsObject[PermissionToRead] = deriveEncoder[PermissionToRead]
  implicit final val permissionToUpdateEncoder: Encoder.AsObject[PermissionToUpdate] = deriveEncoder[PermissionToUpdate]

  implicit final val backOfficeUserToCreateDecoder: Encoder.AsObject[BackOfficeUserToCreate] =
    deriveEncoder[BackOfficeUserToCreate]
  implicit final val backOfficeUserToReadEncoder: Encoder.AsObject[BackOfficeUserToRead] =
    deriveEncoder[BackOfficeUserToRead]
  implicit final val backOfficeUserToUpdateEncoder: Encoder.AsObject[BackOfficeUserToUpdate] =
    deriveEncoder[BackOfficeUserToUpdate]

  implicit final val passwordResetClaimEncoder: Encoder.AsObject[PasswordResetClaim] = deriveEncoder[PasswordResetClaim]
  implicit final val passwordLinkRequestEncoder: Encoder.AsObject[ResetPasswordLinkRequest] =
    deriveEncoder[ResetPasswordLinkRequest]
  implicit final val passwordResetEncoder: Encoder.AsObject[PasswordReset] = deriveEncoder[PasswordReset]

  implicit final val emailEncoder: Encoder[Email] = Encoder.encodeString.contramap[Email](_.value)
  implicit final val credentialsToReadEncoder: Encoder.AsObject[CredentialsToRead] = deriveEncoder[CredentialsToRead]
  implicit final val credentialsToUpdateEncoder: Encoder.AsObject[CredentialsToUpdate] = deriveEncoder[CredentialsToUpdate]
  implicit final val claimContentEncoder: Encoder.AsObject[ClaimContent] = deriveEncoder[ClaimContent]
  implicit final val loginResponseEncoder: Encoder.AsObject[LoginResponse] = deriveEncoder[LoginResponse]
  implicit final val loginStatusResponseEncoder: Encoder.AsObject[LoginStatusResponse] = deriveEncoder[LoginStatusResponse]

  implicit final val healthInfoEncoder: Encoder.AsObject[HealthInfo] = deriveEncoder[HealthInfo]

  implicit final val documentToReadEncoder: Encoder.AsObject[DocumentToRead] = deriveEncoder[DocumentToRead]

  /*
    Idea is to declare page encoders for every type we need here, in the bottom of the file
    rather then use generics and generate them in runtime
  */
  implicit final val documentToReadPageEncoder: Encoder.AsObject[PaginatedResult[DocumentToRead]] =
    deriveEncoder[PaginatedResult[DocumentToRead]]
  implicit final val bouToReadPageEncoder: Encoder.AsObject[PaginatedResult[BackOfficeUserToRead]] =
    deriveEncoder[PaginatedResult[BackOfficeUserToRead]]
  implicit final val buToReadPageEncoder: Encoder.AsObject[PaginatedResult[BusinessUnitToRead]] =
    deriveEncoder[PaginatedResult[BusinessUnitToRead]]
  implicit final val pToReadPageEncoder: Encoder.AsObject[PaginatedResult[PermissionToRead]] =
    deriveEncoder[PaginatedResult[PermissionToRead]]
  implicit final val roleToReadPageEncoder: Encoder.AsObject[PaginatedResult[RoleToRead]] =
    deriveEncoder[PaginatedResult[RoleToRead]]
  implicit final val scopeToReadPageEncoder: Encoder.AsObject[PaginatedResult[ScopeToRead]] =
    deriveEncoder[PaginatedResult[ScopeToRead]]
}
