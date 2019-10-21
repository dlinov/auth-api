package io.dlinov.auth.routes.json

import cats.effect.Sync
import org.http4s.EntityDecoder
import PimpedCirce.jsonOf
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.ClaimContent
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.ClaimContent
import io.dlinov.auth.routes.dto._

trait EntityDecoders[F[_]] {
  import CirceDecoders._

  implicit protected def syncF: Sync[F]

  implicit val scopeToCreateEntityDecoder: EntityDecoder[F, ScopeToCreate] =
    jsonOf[F, ScopeToCreate]
  implicit val scopeToReadEntityDecoder: EntityDecoder[F, ScopeToRead] = jsonOf[F, ScopeToRead]
  implicit val scopeToUpdateEntityDecoder: EntityDecoder[F, ScopeToUpdate] =
    jsonOf[F, ScopeToUpdate]
  implicit val apiErrorEntityDecoder: EntityDecoder[F, ApiError] = jsonOf[F, ApiError]

  implicit val buToCreateEntityDecoder: EntityDecoder[F, BusinessUnitToCreate] =
    jsonOf[F, BusinessUnitToCreate]
  implicit val buToReadEntityDecoder: EntityDecoder[F, BusinessUnitToRead] =
    jsonOf[F, BusinessUnitToRead]
  implicit val buToReadSeqEntityDecoder: EntityDecoder[F, Seq[BusinessUnitToRead]] =
    jsonOf[F, Seq[BusinessUnitToRead]]
  implicit val buToUpdateEntityDecoder: EntityDecoder[F, BusinessUnitToUpdate] =
    jsonOf[F, BusinessUnitToUpdate]

  implicit val roleToCreateEntityDecoder: EntityDecoder[F, RoleToCreate] = jsonOf[F, RoleToCreate]
  implicit val roleToReadEntityDecoder: EntityDecoder[F, RoleToRead]     = jsonOf[F, RoleToRead]
  implicit val roleToReadSeqEntityDecoder: EntityDecoder[F, Seq[RoleToRead]] =
    jsonOf[F, Seq[RoleToRead]]
  implicit val roleToUpdateEntityDecoder: EntityDecoder[F, RoleToUpdate] = jsonOf[F, RoleToUpdate]

  implicit val bouToCreateEntityDecoder: EntityDecoder[F, BackOfficeUserToCreate] =
    jsonOf[F, BackOfficeUserToCreate]
  implicit val bouToReadEntityDecoder: EntityDecoder[F, BackOfficeUserToRead] =
    jsonOf[F, BackOfficeUserToRead]
  implicit val bouToReadSeqEntityDecoder: EntityDecoder[F, Seq[BackOfficeUserToRead]] =
    jsonOf[F, Seq[BackOfficeUserToRead]]
  implicit val bouToUpdateEntityDecoder: EntityDecoder[F, BackOfficeUserToUpdate] =
    jsonOf[F, BackOfficeUserToUpdate]

  implicit val pToCreateEntityDecoder: EntityDecoder[F, PermissionToCreate] =
    jsonOf[F, PermissionToCreate]
  implicit val pToReadEntityDecoder: EntityDecoder[F, PermissionToRead] =
    jsonOf[F, PermissionToRead]
  implicit val pToUpdateEntityDecoder: EntityDecoder[F, PermissionToUpdate] =
    jsonOf[F, PermissionToUpdate]

  implicit val credentialsToReadEntityDecoder: EntityDecoder[F, CredentialsToRead] =
    jsonOf[F, CredentialsToRead]
  implicit val credentialsToUpdateEntityDecoder: EntityDecoder[F, CredentialsToUpdate] =
    jsonOf[F, CredentialsToUpdate]
  implicit val passwordResetEntityDecoder: EntityDecoder[F, PasswordReset] =
    jsonOf[F, PasswordReset]
  implicit val passwordResetLinkRequestEntityDecoder: EntityDecoder[F, ResetPasswordLinkRequest] =
    jsonOf[F, ResetPasswordLinkRequest]

  implicit val loginResponseEntityDecoder: EntityDecoder[F, LoginResponse] =
    jsonOf[F, LoginResponse]
  implicit val loginStatusResponseEntityDecoder: EntityDecoder[F, LoginStatusResponse] =
    jsonOf[F, LoginStatusResponse]
  implicit val claimContentEntityDecoder: EntityDecoder[F, ClaimContent] = jsonOf[F, ClaimContent]

  implicit val documentToCreateEntityDecoder: EntityDecoder[F, DocumentToCreate] =
    jsonOf[F, DocumentToCreate]

  implicit val bouPageEntityDecoder: EntityDecoder[F, PaginatedResult[BackOfficeUserToRead]] =
    jsonOf[F, PaginatedResult[BackOfficeUserToRead]]
  implicit val buPageEntityDecoder: EntityDecoder[F, PaginatedResult[BusinessUnitToRead]] =
    jsonOf[F, PaginatedResult[BusinessUnitToRead]]
  implicit val rolePageEntityDecoder: EntityDecoder[F, PaginatedResult[RoleToRead]] =
    jsonOf[F, PaginatedResult[RoleToRead]]
  implicit val pToReadPageEntityDecoder: EntityDecoder[F, PaginatedResult[PermissionToRead]] =
    jsonOf[F, PaginatedResult[PermissionToRead]]
}
