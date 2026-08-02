package io.dlinov.auth.routes.json

import java.net.URI

import io.circe._
import io.circe.derivation.{Configuration, ConfiguredDecoder}
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.domain.ErrorCodes._
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email, PasswordResetClaim}
import io.dlinov.auth.routes.dto.PermissionKeys.{
  BusinessUnitAndRolePermissionKey,
  UserPermissionKey
}
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

import scala.util.Try
import scala.util.matching.Regex

object CirceDecoders extends CirceConfigProvider {
  implicit override val config: Configuration = CirceConfigProvider.snakeConfig

  implicit val scopeToReadDecoder: Decoder[ScopeToRead]     = ConfiguredDecoder.derived[ScopeToRead]
  implicit val scopeToCreateDecoder: Decoder[ScopeToCreate] = ConfiguredDecoder
    .derived[ScopeToCreate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(_.name.length <= 32, s"name length must not exceed 32 characters")
    .ensure(_.description.forall(_.nonEmpty), "description must not be an empty string")
    .ensure(
      _.description.forall(_.length <= 255),
      "description length must not exceed 255 characters"
    )
  implicit val scopeToUpdateDecoder: Decoder[ScopeToUpdate] = ConfiguredDecoder
    .derived[ScopeToUpdate]
    .ensure(_.description.forall(_.nonEmpty), "description must not be an empty string")
    .ensure(
      _.description.forall(_.length <= 255),
      "description length must not exceed 255 characters"
    )

  // circe-generic-extras' deriveEnumerationDecoder has no Scala 3 / circe 0.14 core equivalent;
  // ErrorCode is a plain enumeration of case objects, so encode/decode it as a snake_case string.
  implicit val errorCodeEncoder: Decoder[ErrorCode] = Decoder.decodeString.emap {
    case "unknown"                    => Right(Unknown)
    case "duplicate_entity"           => Right(DuplicateEntity)
    case "not_found_entity"           => Right(NotFoundEntity)
    case "validation_failed"          => Right(ValidationFailed)
    case "not_authorized"             => Right(NotAuthorized)
    case "captcha_required"           => Right(CaptchaRequired)
    case "invalid_captcha"            => Right(InvalidCaptcha)
    case "invalid_config"             => Right(InvalidConfig)
    case "account_temporarily_locked" => Right(AccountTemporarilyLocked)
    case "permissions_insufficient"   => Right(PermissionsInsufficient)
    case other                        => Left(s"Unknown error code: $other")
  }
  implicit val errorParamsEncoder: Decoder[ErrorParams] = ConfiguredDecoder.derived[ErrorParams]
  implicit val apiErrorDecoder: Decoder[ApiError]       = ConfiguredDecoder.derived[ApiError]

  final private val buNameMaxLength = 32
  private val buNameRegex: Regex    = s"^[A-Za-z0-9_'\\.\\-\\s]{1,$buNameMaxLength}$$".r
  implicit val buToReadDecoder: Decoder[BusinessUnitToRead] =
    ConfiguredDecoder.derived[BusinessUnitToRead]
  implicit val buToCreateDecoder: Decoder[BusinessUnitToCreate] =
    ConfiguredDecoder
      .derived[BusinessUnitToCreate]
      .ensure(_.name.nonEmpty, "name must not be empty")
      .ensure(
        _.name.length <= buNameMaxLength,
        s"name length must not exceed $buNameMaxLength characters"
      )
      .ensure(
        bu => buNameRegex.findFirstMatchIn(bu.name).isDefined,
        s"name must follow pattern $buNameRegex"
      )
  implicit val buToUpdateDecoder: Decoder[BusinessUnitToUpdate] =
    ConfiguredDecoder
      .derived[BusinessUnitToUpdate]
      .ensure(_.name.nonEmpty, "name must not be empty")
      .ensure(
        _.name.length <= buNameMaxLength,
        s"name length must not exceed $buNameMaxLength characters"
      )
      .ensure(
        bu => buNameRegex.findFirstMatchIn(bu.name).isDefined,
        s"name must follow pattern $buNameRegex"
      )

  final private val roleNameMaxLength = 32
  private val roleNameRegex: Regex    = s"^[A-Za-z0-9_\\.\\-\\s]{1,$roleNameMaxLength}$$".r
  implicit val roleToReadDecoder: Decoder[RoleToRead]     = ConfiguredDecoder.derived[RoleToRead]
  implicit val roleToCreateDecoder: Decoder[RoleToCreate] = ConfiguredDecoder
    .derived[RoleToCreate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(
      _.name.length <= roleNameMaxLength,
      s"name length must not exceed $roleNameMaxLength characters"
    )
    .ensure(
      r => roleNameRegex.findFirstMatchIn(r.name).isDefined,
      s"name must follow pattern $roleNameRegex"
    )
  implicit val roleToUpdateDecoder: Decoder[RoleToUpdate] = ConfiguredDecoder
    .derived[RoleToUpdate]
    .ensure(_.name.nonEmpty, "name must not be empty")
    .ensure(
      _.name.length <= roleNameMaxLength,
      s"name length must not exceed $roleNameMaxLength characters"
    )
    .ensure(
      r => roleNameRegex.findFirstMatchIn(r.name).isDefined,
      s"name must follow pattern $roleNameRegex"
    )

  implicit val permissionToReadDecoder: Decoder[PermissionToRead] =
    ConfiguredDecoder.derived[PermissionToRead]

  private val bouUserNameRegex: Regex                          = "^[A-Za-z0-9_\\.]{3,128}$".r
  private val bouNameRegex: Regex                              = "^[A-Za-z'\\.\\-\\s]{1,128}$".r
  implicit val bouToReadDecoder: Decoder[BackOfficeUserToRead] =
    ConfiguredDecoder.derived[BackOfficeUserToRead]
  implicit val bouToCreateDecoder: Decoder[BackOfficeUserToCreate] =
    ConfiguredDecoder
      .derived[BackOfficeUserToCreate]
      .ensure(_.userName.nonEmpty, "username must not be empty")
      .ensure(_.userName.length <= 128, "username length must not exceed 128 characters")
      .ensure(
        u => bouUserNameRegex.findFirstMatchIn(u.userName).isDefined,
        s"username doesn't follow pattern $bouUserNameRegex"
      )
      .ensure(_.firstName.nonEmpty, "first name must not be empty")
      .ensure(_.firstName.length <= 128, "first name length must not exceed 128 characters")
      .ensure(
        u => bouNameRegex.findFirstMatchIn(u.firstName).isDefined,
        s"first name doesn't follow pattern $bouNameRegex"
      )
      .ensure(_.lastName.nonEmpty, "last name must not be empty")
      .ensure(_.lastName.length <= 128, "last name length must not exceed 128 characters")
      .ensure(
        u => bouNameRegex.findFirstMatchIn(u.lastName).isDefined,
        s"last name doesn't follow pattern $bouNameRegex"
      )
      .ensure(_.homePage.forall(_.nonEmpty), "home page must not be an empty string")
      .ensure(
        _.homePage.forall(p => Try(new URI(p).toURL).isSuccess),
        "home page must be a valid URL"
      )
      .ensure(_.activeLanguage.forall(_.nonEmpty), "active language must not be an empty string")
  implicit val bouToUpdateDecoder: Decoder[BackOfficeUserToUpdate] = ConfiguredDecoder
    .derived[BackOfficeUserToUpdate]
    .ensure(_.firstName.forall(_.nonEmpty), "first name must not be empty")
    .ensure(_.firstName.forall(_.length <= 128), "first name length must not exceed 128 characters")
    .ensure(
      _.firstName.forall(bouNameRegex.findFirstMatchIn(_).isDefined),
      s"first name doesn't follow pattern $bouNameRegex"
    )
    .ensure(_.lastName.forall(_.nonEmpty), "last name must not be empty")
    .ensure(_.lastName.forall(_.length <= 128), "last name length must not exceed 128 characters")
    .ensure(
      _.lastName.forall(bouNameRegex.findFirstMatchIn(_).isDefined),
      s"last name doesn't follow pattern $bouNameRegex"
    )
    .ensure(_.homePage.forall(_.nonEmpty), "home page must not be an empty string")
    .ensure(
      _.homePage.forall(p => Try(new URI(p).toURL).isSuccess),
      "home page must be a valid URL"
    )
    .ensure(_.activeLanguage.forall(_.nonEmpty), "active language must not be an empty string")

  implicit val burPKeyDecoder: Decoder[BusinessUnitAndRolePermissionKey] =
    ConfiguredDecoder.derived[BusinessUnitAndRolePermissionKey]
  implicit val uPKeyDecoder: Decoder[UserPermissionKey] =
    ConfiguredDecoder.derived[UserPermissionKey]
  // implicit val pKeyDecoder: Decoder[PermissionKey] = ConfiguredDecoder.derived[PermissionKey]
  implicit val pKeyDecoder: Decoder[PermissionKey] = (c: HCursor) => {
    if (c.keys.forall(_.exists(_ == "role_id"))) {
      burPKeyDecoder(c)
    } else {
      uPKeyDecoder(c)
    }
  }
  implicit val pToCreateDecoder: Decoder[PermissionToCreate] =
    ConfiguredDecoder.derived[PermissionToCreate]
  implicit val pToUpdateDecoder: Decoder[PermissionToUpdate] =
    ConfiguredDecoder.derived[PermissionToUpdate]

  implicit val credentialsToReadDecoder: Decoder[CredentialsToRead] =
    ConfiguredDecoder
      .derived[CredentialsToRead]
      .ensure(_.user.nonEmpty, "user name must not be empty")
      .ensure(_.password.nonEmpty, "password must not be empty")
      .ensure(_.captcha.forall(_.nonEmpty), "captcha must not be an empty string")
  implicit val credentialsToUpdateDecoder: Decoder[CredentialsToUpdate] =
    ConfiguredDecoder
      .derived[CredentialsToUpdate]
      .ensure(_.user.nonEmpty, "user name must not be empty")
      .ensure(_.oldPassword.nonEmpty, "old password must not be empty")
      .ensure(_.newPassword.nonEmpty, "new password must not be empty")
  implicit val loginResponseDecoder: Decoder[LoginResponse] =
    ConfiguredDecoder.derived[LoginResponse]
  implicit val loginStatusResponseDecoder: Decoder[LoginStatusResponse] =
    ConfiguredDecoder.derived[LoginStatusResponse]
  implicit val passwordResetDecoder: Decoder[PasswordReset] = ConfiguredDecoder
    .derived[PasswordReset]
    .ensure(_.password.nonEmpty, "password must not be empty")
    .ensure(_.token.nonEmpty, "token must not be empty")
  implicit val passwordResetClaimDecoder: Decoder[PasswordResetClaim] =
    ConfiguredDecoder
      .derived[PasswordResetClaim]
      .ensure(_.userName.nonEmpty, "user name must not be empty")
  implicit val passwordResetLinkRequestDecoder: Decoder[ResetPasswordLinkRequest] =
    ConfiguredDecoder
      .derived[ResetPasswordLinkRequest]
      .ensure(_.userName.nonEmpty, "user name must not be empty")
      .ensure(_.captcha.forall(_.nonEmpty), "captcha must not be an empty string")

  implicit val emailDecoder: Decoder[Email] = Decoder.decodeString
    .ensure(_.length <= 64, "email length must not exceed 64 characters")
    .ensure(Email.isValid, "provided email is not correct")
    .emapTry(s => Try(Email.createValidated(s)))

  implicit val claimContentDecoder: Decoder[ClaimContent] = ConfiguredDecoder.derived[ClaimContent]

  implicit val bouPageDecoder: Decoder[PaginatedResult[BackOfficeUserToRead]] =
    ConfiguredDecoder.derived[PaginatedResult[BackOfficeUserToRead]]
  implicit val buPageDecoder: Decoder[PaginatedResult[BusinessUnitToRead]] =
    ConfiguredDecoder.derived[PaginatedResult[BusinessUnitToRead]]
  implicit val rolePageDecoder: Decoder[PaginatedResult[RoleToRead]] =
    ConfiguredDecoder.derived[PaginatedResult[RoleToRead]]
  implicit val pToReadPageDecoder: Decoder[PaginatedResult[PermissionToRead]] =
    ConfiguredDecoder.derived[PaginatedResult[PermissionToRead]]

}
