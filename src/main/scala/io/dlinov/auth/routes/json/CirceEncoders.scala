package io.dlinov.auth.routes.json

import io.circe.{Encoder, ObjectEncoder}
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
  implicit final val errorParamsEncoder: ObjectEncoder[ErrorParams] = deriveEncoder[ErrorParams]
  implicit final val apiErrorEncoder: ObjectEncoder[ApiError] = deriveEncoder[ApiError]

  implicit final val scopeToCreateEncoder: ObjectEncoder[ScopeToCreate] = deriveEncoder[ScopeToCreate]
  implicit final val scopeToReadEncoder: ObjectEncoder[ScopeToRead] = deriveEncoder[ScopeToRead]
  implicit final val scopeToUpdateEncoder: ObjectEncoder[ScopeToUpdate] = deriveEncoder[ScopeToUpdate]

  implicit final val roleToCreateEncoder: ObjectEncoder[RoleToCreate] = deriveEncoder[RoleToCreate]
  implicit final val roleToReadEncoder: ObjectEncoder[RoleToRead] = deriveEncoder[RoleToRead]
  implicit final val roleToUpdateEncoder: ObjectEncoder[RoleToUpdate] = deriveEncoder[RoleToUpdate]

  implicit final val businessUnitToCreateEncoder: ObjectEncoder[BusinessUnitToCreate] =
    deriveEncoder[BusinessUnitToCreate]
  implicit final val businessUnitToReadEncoder: ObjectEncoder[BusinessUnitToRead] =
    deriveEncoder[BusinessUnitToRead]
  implicit final val businessUnitToUpdateEncoder: ObjectEncoder[BusinessUnitToUpdate] =
    deriveEncoder[BusinessUnitToUpdate]

  implicit final val burPKeyEncoder: ObjectEncoder[BusinessUnitAndRolePermissionKey] =
    deriveEncoder[BusinessUnitAndRolePermissionKey]
  implicit final val uPKeyEncoder: ObjectEncoder[UserPermissionKey] =
    deriveEncoder[UserPermissionKey]
  implicit final val pKeyEncoder: ObjectEncoder[PermissionKey] =
    deriveEncoder[PermissionKey].mapJsonObject(j ⇒ j(j.keys.head).get.asObject.get)
  implicit final val permissionToCreateEncoder: ObjectEncoder[PermissionToCreate] = deriveEncoder[PermissionToCreate]
  implicit final val permissionToReadEncoder: ObjectEncoder[PermissionToRead] = deriveEncoder[PermissionToRead]
  implicit final val permissionToUpdateEncoder: ObjectEncoder[PermissionToUpdate] = deriveEncoder[PermissionToUpdate]

  implicit final val backOfficeUserToCreateDecoder: ObjectEncoder[BackOfficeUserToCreate] =
    deriveEncoder[BackOfficeUserToCreate]
  implicit final val backOfficeUserToReadEncoder: ObjectEncoder[BackOfficeUserToRead] =
    deriveEncoder[BackOfficeUserToRead]
  implicit final val backOfficeUserToUpdateEncoder: ObjectEncoder[BackOfficeUserToUpdate] =
    deriveEncoder[BackOfficeUserToUpdate]

  implicit final val passwordResetClaimEncoder: ObjectEncoder[PasswordResetClaim] = deriveEncoder[PasswordResetClaim]
  implicit final val passwordLinkRequestEncoder: ObjectEncoder[ResetPasswordLinkRequest] =
    deriveEncoder[ResetPasswordLinkRequest]
  implicit final val passwordResetEncoder: ObjectEncoder[PasswordReset] = deriveEncoder[PasswordReset]

  implicit final val emailEncoder: Encoder[Email] = Encoder.encodeString.contramap[Email](_.value)
  implicit final val credentialsToReadEncoder: ObjectEncoder[CredentialsToRead] = deriveEncoder[CredentialsToRead]
  implicit final val credentialsToUpdateEncoder: ObjectEncoder[CredentialsToUpdate] = deriveEncoder[CredentialsToUpdate]
  implicit final val claimContentEncoder: ObjectEncoder[ClaimContent] = deriveEncoder[ClaimContent]
  implicit final val loginResponseEncoder: ObjectEncoder[LoginResponse] = deriveEncoder[LoginResponse]
  implicit final val loginStatusResponseEncoder: ObjectEncoder[LoginStatusResponse] = deriveEncoder[LoginStatusResponse]

  implicit final val healthInfoEncoder: ObjectEncoder[HealthInfo] = deriveEncoder[HealthInfo]

  implicit final val documentToReadEncoder: ObjectEncoder[DocumentToRead] = deriveEncoder[DocumentToRead]

  /*
    Idea is to declare page encoders for every type we need here, in the bottom of the file
    rather then use generics and generate them in runtime
  */
  implicit final val documentToReadPageEncoder: ObjectEncoder[PaginatedResult[DocumentToRead]] =
    deriveEncoder[PaginatedResult[DocumentToRead]]
  implicit final val bouToReadPageEncoder: ObjectEncoder[PaginatedResult[BackOfficeUserToRead]] =
    deriveEncoder[PaginatedResult[BackOfficeUserToRead]]
  implicit final val buToReadPageEncoder: ObjectEncoder[PaginatedResult[BusinessUnitToRead]] =
    deriveEncoder[PaginatedResult[BusinessUnitToRead]]
  implicit final val pToReadPageEncoder: ObjectEncoder[PaginatedResult[PermissionToRead]] =
    deriveEncoder[PaginatedResult[PermissionToRead]]
  implicit final val roleToReadPageEncoder: ObjectEncoder[PaginatedResult[RoleToRead]] =
    deriveEncoder[PaginatedResult[RoleToRead]]
  implicit final val scopeToReadPageEncoder: ObjectEncoder[PaginatedResult[ScopeToRead]] =
    deriveEncoder[PaginatedResult[ScopeToRead]]
}
