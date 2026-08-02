package io.dlinov.auth.routes.docs

import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import io.dlinov.auth.domain.{ErrorCode, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import io.dlinov.auth.routes.dto._
import io.dlinov.auth.routes.dto.PermissionKeys.{
  BusinessUnitAndRolePermissionKey,
  UserPermissionKey
}
import io.dlinov.auth.routes.json.CirceConfigProvider
import sttp.tapir.Schema

/** Round-trip circe `Codec`s for every DTO referenced by `ApiEndpoints`, purely to satisfy Tapir's
  * `jsonBody[T: Encoder: Decoder: Schema]` bound, which - unlike the application's own
  * one-directional `CirceEncoders`/`CirceDecoders` (most request DTOs only ever need decoding, most
  * response DTOs only ever need encoding) - requires both directions to be in scope regardless of
  * which one a particular body actually uses at runtime.
  *
  * These are documentation-only instances used solely to build the generated OpenAPI schema;
  * derived with the same snake_case naming convention actually used at runtime
  * (`CirceConfigProvider.snakeConfig`), but otherwise independent of, and not used by, any
  * request-handling code (`CirceEncoders`/`CirceDecoders` are unaffected).
  */
object DocsCodecs {
  implicit val config: io.circe.derivation.Configuration = CirceConfigProvider.snakeConfig

  // Email/ErrorCode are rendered as plain JSON strings at runtime (see CirceEncoders/
  // CirceDecoders), not as objects, so their doc codec/schema mirror that rather than being
  // auto-derived as a product/coproduct.
  implicit val emailEncoder: io.circe.Encoder[Email] =
    io.circe.Encoder.encodeString.contramap(_.value)
  implicit val emailDecoder: io.circe.Decoder[Email] =
    io.circe.Decoder.decodeString.map(Email.apply)
  implicit val emailSchema: Schema[Email] = Schema.string

  implicit val errorCodeEncoder: io.circe.Encoder[ErrorCode] =
    io.circe.Encoder.encodeString.contramap(_.getClass.getSimpleName.stripSuffix("$"))
  implicit val errorCodeDecoder: io.circe.Decoder[ErrorCode] =
    io.circe.Decoder.decodeString.map(_ => io.dlinov.auth.domain.ErrorCodes.Unknown)
  implicit val errorCodeSchema: Schema[ErrorCode] = Schema.string

  implicit val healthInfoCodec: Codec[HealthInfo]               = ConfiguredCodec.derived
  implicit val apiErrorParamsCodec: Codec[ApiError.ErrorParams] = ConfiguredCodec.derived
  implicit val apiErrorCodec: Codec[ApiError]                   = ConfiguredCodec.derived

  implicit val credentialsToReadCodec: Codec[CredentialsToRead]     = ConfiguredCodec.derived
  implicit val credentialsToUpdateCodec: Codec[CredentialsToUpdate] = ConfiguredCodec.derived
  implicit val loginResponseCodec: Codec[LoginResponse]             = ConfiguredCodec.derived
  implicit val loginStatusResponseCodec: Codec[LoginStatusResponse] = ConfiguredCodec.derived
  implicit val passwordResetCodec: Codec[PasswordReset]             = ConfiguredCodec.derived
  implicit val resetPasswordLinkRequestCodec: Codec[ResetPasswordLinkRequest] =
    ConfiguredCodec.derived

  implicit val scopeToCreateCodec: Codec[ScopeToCreate] = ConfiguredCodec.derived
  implicit val scopeToReadCodec: Codec[ScopeToRead]     = ConfiguredCodec.derived
  implicit val scopeToUpdateCodec: Codec[ScopeToUpdate] = ConfiguredCodec.derived

  implicit val roleToCreateCodec: Codec[RoleToCreate] = ConfiguredCodec.derived
  implicit val roleToReadCodec: Codec[RoleToRead]     = ConfiguredCodec.derived
  implicit val roleToUpdateCodec: Codec[RoleToUpdate] = ConfiguredCodec.derived

  implicit val businessUnitToCreateCodec: Codec[BusinessUnitToCreate] = ConfiguredCodec.derived
  implicit val businessUnitToReadCodec: Codec[BusinessUnitToRead]     = ConfiguredCodec.derived
  implicit val businessUnitToUpdateCodec: Codec[BusinessUnitToUpdate] = ConfiguredCodec.derived

  implicit val burPermissionKeyCodec: Codec[BusinessUnitAndRolePermissionKey] =
    ConfiguredCodec.derived
  implicit val userPermissionKeyCodec: Codec[UserPermissionKey] = ConfiguredCodec.derived
  implicit val permissionKeyCodec: Codec[PermissionKey]         = ConfiguredCodec.derived

  implicit val permissionToCreateCodec: Codec[PermissionToCreate] = ConfiguredCodec.derived
  implicit val permissionToReadCodec: Codec[PermissionToRead]     = ConfiguredCodec.derived
  implicit val permissionToUpdateCodec: Codec[PermissionToUpdate] = ConfiguredCodec.derived

  implicit val backOfficeUserToCreateCodec: Codec[BackOfficeUserToCreate] = ConfiguredCodec.derived
  implicit val backOfficeUserToReadCodec: Codec[BackOfficeUserToRead]     = ConfiguredCodec.derived
  implicit val backOfficeUserToUpdateCodec: Codec[BackOfficeUserToUpdate] = ConfiguredCodec.derived

  implicit def paginatedResultCodec[T: io.circe.Encoder: io.circe.Decoder]
      : Codec[PaginatedResult[T]] = ConfiguredCodec.derived
}
