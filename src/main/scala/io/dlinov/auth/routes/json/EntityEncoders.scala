package io.dlinov.auth.routes.json

import cats.effect.IO
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email}
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto.{ApiError, BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate, BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate, CredentialsToRead, CredentialsToUpdate, LoginResponse, LoginStatusResponse, PasswordReset, PermissionToCreate, PermissionToRead, PermissionToUpdate, ResetPasswordLinkRequest, RoleToCreate, RoleToRead, RoleToUpdate, ScopeToCreate, ScopeToRead, ScopeToUpdate}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email}
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto.{ResetPasswordLinkRequest, _}

object EntityEncoders {
  import CirceEncoders._

  implicit final val scopeToCreateEntityEncoder: EntityEncoder[IO, ScopeToCreate] = jsonEncoderOf[IO, ScopeToCreate]
  implicit final val scopeToReadEntityEncoder: EntityEncoder[IO, ScopeToRead] = jsonEncoderOf[IO, ScopeToRead]
  implicit final val scopeToUpdateEntityEncoder: EntityEncoder[IO, ScopeToUpdate] = jsonEncoderOf[IO, ScopeToUpdate]
  implicit final val apiErrorEntityEncoder: EntityEncoder[IO, ApiError] = jsonEncoderOf[IO, ApiError]

  implicit final val roleToCreateEntityEncoder: EntityEncoder[IO, RoleToCreate] = jsonEncoderOf[IO, RoleToCreate]
  implicit final val roleToReadEntityEncoder: EntityEncoder[IO, RoleToRead] = jsonEncoderOf[IO, RoleToRead]
  implicit final val roleToUpdateEntityEncoder: EntityEncoder[IO, RoleToUpdate] = jsonEncoderOf[IO, RoleToUpdate]

  implicit final val businessUnitToCreateEntityEncoder: EntityEncoder[IO, BusinessUnitToCreate] =
    jsonEncoderOf[IO, BusinessUnitToCreate]
  implicit final val businessUnitToReadEntityEncoder: EntityEncoder[IO, BusinessUnitToRead] =
    jsonEncoderOf[IO, BusinessUnitToRead]
  implicit final val businessUnitToUpdateEntityEncoder: EntityEncoder[IO, BusinessUnitToUpdate] =
    jsonEncoderOf[IO, BusinessUnitToUpdate]

  implicit final val permissionToCreateEntityEncoder: EntityEncoder[IO, PermissionToCreate] =
    jsonEncoderOf[IO, PermissionToCreate]
  implicit final val permissionToReadEntityEncoder: EntityEncoder[IO, PermissionToRead] =
    jsonEncoderOf[IO, PermissionToRead]
  implicit final val permissionToUpdateEntityEncoder: EntityEncoder[IO, PermissionToUpdate] =
    jsonEncoderOf[IO, PermissionToUpdate]

  implicit final val backOfficeUserToCreateEntityEncoder: EntityEncoder[IO, BackOfficeUserToCreate] =
    jsonEncoderOf[IO, BackOfficeUserToCreate]
  implicit final val backOfficeUserToReadEntityEncoder: EntityEncoder[IO, BackOfficeUserToRead] =
    jsonEncoderOf[IO, BackOfficeUserToRead]
  implicit final val backOfficeUserToUpdateEntityEncoder: EntityEncoder[IO, BackOfficeUserToUpdate] =
    jsonEncoderOf[IO, BackOfficeUserToUpdate]

  implicit final val emailEntityEncoder: EntityEncoder[IO, Email] = jsonEncoderOf[IO, Email]
  implicit final val credentialsToReadEntityEncoder: EntityEncoder[IO, CredentialsToRead] =
    jsonEncoderOf[IO, CredentialsToRead]
  implicit final val credentialsToUpdateEntityEncoder: EntityEncoder[IO, CredentialsToUpdate] =
    jsonEncoderOf[IO, CredentialsToUpdate]
  implicit final val passwordResetEntityDecoder: EntityEncoder[IO, PasswordReset] =
    jsonEncoderOf[IO, PasswordReset]
  implicit final val passwordLinkRequestEntityEncoder: EntityEncoder[IO, ResetPasswordLinkRequest] =
    jsonEncoderOf[IO, ResetPasswordLinkRequest]

  implicit final val claimContentEntityEncoder: EntityEncoder[IO, ClaimContent] = jsonEncoderOf[IO, ClaimContent]
  implicit final val loginResponseEntityEncoder: EntityEncoder[IO, LoginResponse] = jsonEncoderOf[IO, LoginResponse]
  implicit final val loginStatusResponseEntityEncoder: EntityEncoder[IO, LoginStatusResponse] =
    jsonEncoderOf[IO, LoginStatusResponse]
  implicit final val healthInfoEntityEncoder: EntityEncoder[IO, HealthInfo] = jsonEncoderOf[IO, HealthInfo]
}
