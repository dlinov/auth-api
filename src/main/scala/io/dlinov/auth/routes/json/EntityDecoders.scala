package io.dlinov.auth.routes.json

import cats.effect.IO
import io.circe.Decoder
import org.http4s.EntityDecoder
import PimpedCirce.jsonOf
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.ClaimContent
import io.dlinov.auth.routes.dto.{ApiError, BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate, BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate, CredentialsToRead, CredentialsToUpdate, DocumentToCreate, LoginResponse, LoginStatusResponse, PasswordReset, PermissionToCreate, PermissionToRead, PermissionToUpdate, ResetPasswordLinkRequest, RoleToCreate, RoleToRead, RoleToUpdate, ScopeToCreate, ScopeToRead, ScopeToUpdate}
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.ClaimContent
import io.dlinov.auth.routes.dto._

object EntityDecoders {
  import CirceDecoders._

  implicit val scopeToCreateEntityDecoder: EntityDecoder[IO, ScopeToCreate] = jsonOf[IO, ScopeToCreate]
  implicit val scopeToReadEntityDecoder: EntityDecoder[IO, ScopeToRead] = jsonOf[IO, ScopeToRead]
  implicit val scopeToUpdateEntityDecoder: EntityDecoder[IO, ScopeToUpdate] = jsonOf[IO, ScopeToUpdate]
  implicit val apiErrorEntityDecoder: EntityDecoder[IO, ApiError] = jsonOf[IO, ApiError]

  implicit val buToCreateEntityDecoder: EntityDecoder[IO, BusinessUnitToCreate] = jsonOf[IO, BusinessUnitToCreate]
  implicit val buToReadEntityDecoder: EntityDecoder[IO, BusinessUnitToRead] = jsonOf[IO, BusinessUnitToRead]
  implicit val buToReadSeqEntityDecoder: EntityDecoder[IO, Seq[BusinessUnitToRead]] =
    jsonOf[IO, Seq[BusinessUnitToRead]]
  implicit val buToUpdateEntityDecoder: EntityDecoder[IO, BusinessUnitToUpdate] = jsonOf[IO, BusinessUnitToUpdate]

  implicit val roleToCreateEntityDecoder: EntityDecoder[IO, RoleToCreate] = jsonOf[IO, RoleToCreate]
  implicit val roleToReadEntityDecoder: EntityDecoder[IO, RoleToRead] = jsonOf[IO, RoleToRead]
  implicit val roleToReadSeqEntityDecoder: EntityDecoder[IO, Seq[RoleToRead]] = jsonOf[IO, Seq[RoleToRead]]
  implicit val roleToUpdateEntityDecoder: EntityDecoder[IO, RoleToUpdate] = jsonOf[IO, RoleToUpdate]

  implicit val bouToCreateEntityDecoder: EntityDecoder[IO, BackOfficeUserToCreate] = jsonOf[IO, BackOfficeUserToCreate]
  implicit val bouToReadEntityDecoder: EntityDecoder[IO, BackOfficeUserToRead] = jsonOf[IO, BackOfficeUserToRead]
  implicit val bouToReadSeqEntityDecoder: EntityDecoder[IO, Seq[BackOfficeUserToRead]] =
    jsonOf[IO, Seq[BackOfficeUserToRead]]
  implicit val bouToUpdateEntityDecoder: EntityDecoder[IO, BackOfficeUserToUpdate] = jsonOf[IO, BackOfficeUserToUpdate]

  implicit val pToCreateEntityDecoder: EntityDecoder[IO, PermissionToCreate] = jsonOf[IO, PermissionToCreate]
  implicit val pToReadEntityDecoder: EntityDecoder[IO, PermissionToRead] = jsonOf[IO, PermissionToRead]
  implicit val pToUpdateEntityDecoder: EntityDecoder[IO, PermissionToUpdate] = jsonOf[IO, PermissionToUpdate]

  implicit val credentialsToReadEntityDecoder: EntityDecoder[IO, CredentialsToRead] =
    jsonOf[IO, CredentialsToRead]
  implicit val credentialsToUpdateEntityDecoder: EntityDecoder[IO, CredentialsToUpdate] =
    jsonOf[IO, CredentialsToUpdate]
  implicit val passwordResetEntityDecoder: EntityDecoder[IO, PasswordReset] =
    jsonOf[IO, PasswordReset]
  implicit val passwordResetLinkRequestEntityDecoder: EntityDecoder[IO, ResetPasswordLinkRequest] =
    jsonOf[IO, ResetPasswordLinkRequest]

  implicit val loginResponseEntityDecoder: EntityDecoder[IO, LoginResponse] = jsonOf[IO, LoginResponse]
  implicit val loginStatusResponseEntityDecoder: EntityDecoder[IO, LoginStatusResponse] =
    jsonOf[IO, LoginStatusResponse]
  implicit val claimContentEntityDecoder: EntityDecoder[IO, ClaimContent] = jsonOf[IO, ClaimContent]

  implicit val documentToCreateEntityDecoder: EntityDecoder[IO, DocumentToCreate] = jsonOf[IO, DocumentToCreate]

  def pageEntityDecoder[T](implicit dec: Decoder[T]): EntityDecoder[IO, PaginatedResult[T]] = {
    implicit val tPageDecoder: Decoder[PaginatedResult[T]] = pageDecoder[T]
    jsonOf[IO, PaginatedResult[T]]
  }

  implicit val bouPageEntityDecoder: EntityDecoder[IO, PaginatedResult[BackOfficeUserToRead]] =
    pageEntityDecoder[BackOfficeUserToRead]
  implicit val buPageEntityDecoder: EntityDecoder[IO, PaginatedResult[BusinessUnitToRead]] =
    pageEntityDecoder[BusinessUnitToRead]
  implicit val rolePageEntityDecoder: EntityDecoder[IO, PaginatedResult[RoleToRead]] =
    pageEntityDecoder[RoleToRead]
  implicit val pToReadPageEntityDecoder: EntityDecoder[IO, PaginatedResult[PermissionToRead]] =
    pageEntityDecoder[PermissionToRead]
}
