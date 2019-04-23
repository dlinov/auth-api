package io.dlinov.auth.routes.json

import java.net.URL

import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email, PasswordResetClaim}
import io.dlinov.auth.routes.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import io.dlinov.auth.routes.dto.{ApiError, BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate, BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate, CredentialsToRead, CredentialsToUpdate, DocumentToCreate, LoginResponse, LoginStatusResponse, PasswordReset, PermissionKey, PermissionToCreate, PermissionToRead, PermissionToUpdate, ResetPasswordLinkRequest, RoleToCreate, RoleToRead, RoleToUpdate, ScopeToCreate, ScopeToRead, ScopeToUpdate}
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.routes.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email, PasswordResetClaim}
import io.dlinov.auth.routes.dto.ApiError.ErrorParams
import io.dlinov.auth.routes.dto._

import scala.util.Try
import scala.util.matching.Regex

object CirceDecoders extends CirceConfigProvider {
  override implicit val config: Configuration = CirceConfigProvider.snakeConfig

  implicit val scopeToReadDecoder: Decoder[ScopeToRead] = deriveDecoder[ScopeToRead]
  implicit val scopeToCreateDecoder: Decoder[ScopeToCreate] = deriveDecoder[ScopeToCreate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(_.name.length <= 32, s"name length must not exceed 32 characters")
    .ensure(_.description.forall(_.nonEmpty), "description must not be an empty string")
    .ensure(_.description.forall(_.length <= 255), "description length must not exceed 255 characters")
  implicit val scopeToUpdateDecoder: Decoder[ScopeToUpdate] = deriveDecoder[ScopeToUpdate]
    .ensure(_.description.forall(_.nonEmpty), "description must not be an empty string")
    .ensure(_.description.forall(_.length <= 255), "description length must not exceed 255 characters")

  implicit val errorCodeEncoder: Decoder[ErrorCode] = deriveEnumerationDecoder[ErrorCode]
  implicit val errorParamsEncoder: Decoder[ErrorParams] = deriveDecoder[ErrorParams]
  implicit val apiErrorDecoder: Decoder[ApiError] = deriveDecoder[ApiError]

  private final val buNameMaxLength = 32
  private val buNameRegex: Regex = s"^[A-Za-z0-9_'\\.\\-\\s]{1,$buNameMaxLength}$$".r
  implicit val buToReadDecoder: Decoder[BusinessUnitToRead] = deriveDecoder[BusinessUnitToRead]
  implicit val buToCreateDecoder: Decoder[BusinessUnitToCreate] = deriveDecoder[BusinessUnitToCreate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(_.name.length <= buNameMaxLength, s"name length must not exceed $buNameMaxLength characters")
    .ensure(bu ⇒ buNameRegex.findFirstMatchIn(bu.name).isDefined, s"name must follow pattern $buNameRegex")
  implicit val buToUpdateDecoder: Decoder[BusinessUnitToUpdate] = deriveDecoder[BusinessUnitToUpdate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(_.name.length <= buNameMaxLength, s"name length must not exceed $buNameMaxLength characters")
    .ensure(bu ⇒ buNameRegex.findFirstMatchIn(bu.name).isDefined, s"name must follow pattern $buNameRegex")

  private final val roleNameMaxLength = 32
  private val roleNameRegex: Regex = s"^[A-Za-z0-9_\\.\\-\\s]{1,$roleNameMaxLength}$$".r
  implicit val roleToReadDecoder: Decoder[RoleToRead] = deriveDecoder[RoleToRead]
  implicit val roleToCreateDecoder: Decoder[RoleToCreate] = deriveDecoder[RoleToCreate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(_.name.length <= roleNameMaxLength, s"name length must not exceed $roleNameMaxLength characters")
    .ensure(r ⇒ roleNameRegex.findFirstMatchIn(r.name).isDefined, s"name must follow pattern $roleNameRegex")
  implicit val roleToUpdateDecoder: Decoder[RoleToUpdate] = deriveDecoder[RoleToUpdate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(_.name.length <= roleNameMaxLength, s"name length must not exceed $roleNameMaxLength characters")
    .ensure(r ⇒ roleNameRegex.findFirstMatchIn(r.name).isDefined, s"name must follow pattern $roleNameRegex")

  implicit val permissionToReadDecoder: Decoder[PermissionToRead] = deriveDecoder[PermissionToRead]

  private val bouUserNameRegex: Regex = "^[A-Za-z0-9_\\.]{3,128}$".r
  private val bouNameRegex: Regex = "^[A-Za-z'\\.\\-\\s]{1,128}$".r
  implicit val bouToReadDecoder: Decoder[BackOfficeUserToRead] = deriveDecoder[BackOfficeUserToRead]
  implicit val bouToCreateDecoder: Decoder[BackOfficeUserToCreate] = deriveDecoder[BackOfficeUserToCreate]
    .ensure(_.userName.nonEmpty, "username must not be empty")
    .ensure(_.userName.length <= 128, "username length must not exceed 128 characters")
    .ensure(
      u ⇒ bouUserNameRegex.findFirstMatchIn(u.userName).isDefined,
      s"username doesn't follow pattern $bouUserNameRegex")
    .ensure(_.firstName.nonEmpty, "first name must not be empty")
    .ensure(_.firstName.length <= 128, "first name length must not exceed 128 characters")
    .ensure(
      u ⇒ bouNameRegex.findFirstMatchIn(u.firstName).isDefined,
      s"first name doesn't follow pattern $bouNameRegex")
    .ensure(_.lastName.nonEmpty, "last name must not be empty")
    .ensure(_.lastName.length <= 128, "last name length must not exceed 128 characters")
    .ensure(
      u ⇒ bouNameRegex.findFirstMatchIn(u.lastName).isDefined,
      s"last name doesn't follow pattern $bouNameRegex")
    .ensure(_.homePage.forall(_.nonEmpty), "home page must not be an empty string")
    .ensure(_.homePage.forall(p ⇒ Try(new URL(p)).isSuccess), "home page must not be a valid URL")
    .ensure(_.activeLanguage.forall(_.nonEmpty), "active language must not be an empty string")
  implicit val bouToUpdateDecoder: Decoder[BackOfficeUserToUpdate] = deriveDecoder[BackOfficeUserToUpdate]
    .ensure(_.firstName.forall(_.nonEmpty), "first name must not be empty")
    .ensure(_.firstName.forall(_.length <= 128), "first name length must not exceed 128 characters")
    .ensure(
      _.firstName.forall(bouNameRegex.findFirstMatchIn(_).isDefined),
      s"first name doesn't follow pattern $bouNameRegex")
    .ensure(_.lastName.forall(_.nonEmpty), "last name must not be empty")
    .ensure(_.lastName.forall(_.length <= 128), "last name length must not exceed 128 characters")
    .ensure(
      _.lastName.forall(bouNameRegex.findFirstMatchIn(_).isDefined),
      s"last name doesn't follow pattern $bouNameRegex")
    .ensure(_.homePage.forall(_.nonEmpty), "home page must not be an empty string")
    .ensure(_.homePage.forall(p ⇒ Try(new URL(p)).isSuccess), "home page must not be a valid URL")
    .ensure(_.activeLanguage.forall(_.nonEmpty), "active language must not be an empty string")

  implicit val burPKeyDecoder: Decoder[BusinessUnitAndRolePermissionKey] =
    deriveDecoder[BusinessUnitAndRolePermissionKey]
  implicit val uPKeyDecoder: Decoder[UserPermissionKey] = deriveDecoder[UserPermissionKey]
  // implicit val pKeyDecoder: Decoder[PermissionKey] = deriveDecoder[PermissionKey]
  implicit val pKeyDecoder: Decoder[PermissionKey] = (c: HCursor) ⇒ {
    if (c.keys.forall(_.exists(_ == "role_id"))) {
      burPKeyDecoder(c)
    } else {
      uPKeyDecoder(c)
    }
  }
  implicit val pToCreateDecoder: Decoder[PermissionToCreate] = deriveDecoder[PermissionToCreate]
  implicit val pToUpdateDecoder: Decoder[PermissionToUpdate] = deriveDecoder[PermissionToUpdate]

  implicit val credentialsToReadDecoder: Decoder[CredentialsToRead] = deriveDecoder[CredentialsToRead]
    .ensure(_.user.nonEmpty, "user name must not be empty")
    .ensure(_.password.nonEmpty, "password must not be empty")
    .ensure(_.captcha.forall(_.nonEmpty), "captcha must not be an empty string")
  implicit val credentialsToUpdateDecoder: Decoder[CredentialsToUpdate] = deriveDecoder[CredentialsToUpdate]
    .ensure(_.user.nonEmpty, "user name must not be empty")
    .ensure(_.oldPassword.nonEmpty, "old password must not be empty")
    .ensure(_.newPassword.nonEmpty, "new password must not be empty")
  implicit val loginResponseDecoder: Decoder[LoginResponse] = deriveDecoder[LoginResponse]
  implicit val loginStatusResponseDecoder: Decoder[LoginStatusResponse] = deriveDecoder[LoginStatusResponse]
  implicit val passwordResetDecoder: Decoder[PasswordReset] = deriveDecoder[PasswordReset]
    .ensure(_.password.nonEmpty, "password must not be empty")
    .ensure(_.token.nonEmpty, "token must not be empty")
  implicit val passwordResetClaimDecoder: Decoder[PasswordResetClaim] = deriveDecoder[PasswordResetClaim]
    .ensure(_.userName.nonEmpty, "user name must not be empty")
  implicit val passwordResetLinkRequestDecoder: Decoder[ResetPasswordLinkRequest] =
    deriveDecoder[ResetPasswordLinkRequest]
      .ensure(_.userName.nonEmpty, "user name must not be empty")
      .ensure(_.captcha.forall(_.nonEmpty), "captcha must not be an empty string")

  implicit val emailDecoder: Decoder[Email] = Decoder.decodeString
    .ensure(_.length <= 64, "email length must not exceed 64 characters")
    .ensure(Email.isValid, "provided email is not correct")
    .emapTry(s ⇒ Try(Email.createValidated(s)))

  implicit val claimContentDecoder: Decoder[ClaimContent] = deriveDecoder[ClaimContent]

  implicit val documentToCreateDecoder: Decoder[DocumentToCreate] = deriveDecoder[DocumentToCreate]

  def pageDecoder[T](implicit dec: Decoder[T]): Decoder[PaginatedResult[T]] = {
    deriveDecoder[PaginatedResult[T]]
  }

}
