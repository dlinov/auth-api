package io.dlinov.auth.routes.json

import cats.Applicative
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email}
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
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email}
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto.{ResetPasswordLinkRequest, _}

trait EntityEncoders[F[_]] {
  import CirceEncoders._

  implicit protected def applicativeF: Applicative[F]

  implicit final val scopeToCreateEntityEncoder: EntityEncoder[F, ScopeToCreate] =
    jsonEncoderOf[F, ScopeToCreate]
  implicit final val scopeToReadEntityEncoder: EntityEncoder[F, ScopeToRead] =
    jsonEncoderOf[F, ScopeToRead]
  implicit final val scopeToUpdateEntityEncoder: EntityEncoder[F, ScopeToUpdate] =
    jsonEncoderOf[F, ScopeToUpdate]
  implicit final val apiErrorEntityEncoder: EntityEncoder[F, ApiError] = jsonEncoderOf[F, ApiError]

  implicit final val roleToCreateEntityEncoder: EntityEncoder[F, RoleToCreate] =
    jsonEncoderOf[F, RoleToCreate]
  implicit final val roleToReadEntityEncoder: EntityEncoder[F, RoleToRead] =
    jsonEncoderOf[F, RoleToRead]
  implicit final val roleToUpdateEntityEncoder: EntityEncoder[F, RoleToUpdate] =
    jsonEncoderOf[F, RoleToUpdate]

  implicit final val businessUnitToCreateEntityEncoder: EntityEncoder[F, BusinessUnitToCreate] =
    jsonEncoderOf[F, BusinessUnitToCreate]
  implicit final val businessUnitToReadEntityEncoder: EntityEncoder[F, BusinessUnitToRead] =
    jsonEncoderOf[F, BusinessUnitToRead]
  implicit final val businessUnitToUpdateEntityEncoder: EntityEncoder[F, BusinessUnitToUpdate] =
    jsonEncoderOf[F, BusinessUnitToUpdate]

  implicit final val permissionToCreateEntityEncoder: EntityEncoder[F, PermissionToCreate] =
    jsonEncoderOf[F, PermissionToCreate]
  implicit final val permissionToReadEntityEncoder: EntityEncoder[F, PermissionToRead] =
    jsonEncoderOf[F, PermissionToRead]
  implicit final val permissionToUpdateEntityEncoder: EntityEncoder[F, PermissionToUpdate] =
    jsonEncoderOf[F, PermissionToUpdate]

  implicit final val backOfficeUserToCreateEntityEncoder: EntityEncoder[F, BackOfficeUserToCreate] =
    jsonEncoderOf[F, BackOfficeUserToCreate]
  implicit final val backOfficeUserToReadEntityEncoder: EntityEncoder[F, BackOfficeUserToRead] =
    jsonEncoderOf[F, BackOfficeUserToRead]
  implicit final val backOfficeUserToUpdateEntityEncoder: EntityEncoder[F, BackOfficeUserToUpdate] =
    jsonEncoderOf[F, BackOfficeUserToUpdate]

  implicit final val emailEntityEncoder: EntityEncoder[F, Email] = jsonEncoderOf[F, Email]
  implicit final val credentialsToReadEntityEncoder: EntityEncoder[F, CredentialsToRead] =
    jsonEncoderOf[F, CredentialsToRead]
  implicit final val credentialsToUpdateEntityEncoder: EntityEncoder[F, CredentialsToUpdate] =
    jsonEncoderOf[F, CredentialsToUpdate]
  implicit final val passwordResetEntityEncoder: EntityEncoder[F, PasswordReset] =
    jsonEncoderOf[F, PasswordReset]
  implicit final val passwordLinkRequestEntityEncoder: EntityEncoder[F, ResetPasswordLinkRequest] =
    jsonEncoderOf[F, ResetPasswordLinkRequest]

  implicit final val claimContentEntityEncoder: EntityEncoder[F, ClaimContent] =
    jsonEncoderOf[F, ClaimContent]
  implicit final val loginResponseEntityEncoder: EntityEncoder[F, LoginResponse] =
    jsonEncoderOf[F, LoginResponse]
  implicit final val loginStatusResponseEntityEncoder: EntityEncoder[F, LoginStatusResponse] =
    jsonEncoderOf[F, LoginStatusResponse]
  implicit final val healthInfoEntityEncoder: EntityEncoder[F, HealthInfo] =
    jsonEncoderOf[F, HealthInfo]
}
