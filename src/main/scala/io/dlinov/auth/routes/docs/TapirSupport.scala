package io.dlinov.auth.routes.docs

import io.dlinov.auth.routes.dto.ApiError
import sttp.tapir._
import sttp.tapir.generic.Configuration
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import DocsCodecs._

/** Shared Tapir wiring used by every endpoint definition in `ApiEndpoints`.
  *
  * The application's circe codecs (see `CirceConfigProvider`) render/parse JSON using snake_case
  * field names; `withSnakeCaseMemberNames` makes Tapir's own auto-derived `Schema`s (and therefore
  * the generated OpenAPI document) agree with that, rather than defaulting to the raw (camelCase)
  * Scala field names.
  */
object TapirSupport {
  implicit val tapirNamingConfiguration: Configuration =
    Configuration.default.withSnakeCaseMemberNames

  implicit val apiErrorSchema: Schema[ApiError] = Schema.derived[ApiError]

  /** Every failure response in this API (see `Routes.handleError`) is a JSON `ApiError`, tagged
    * with whichever HTTP status the underlying `ErrorCode` maps to; documented generically here
    * rather than re-deriving the exact status per branch for every single endpoint.
    */
  val errorOutput: EndpointOutput[ApiError] = jsonBody[ApiError]

  /** Every authenticated route in this service is guarded by the same `Authorization: Bearer
    * <token>` middleware (`AuthenticationMiddlewareProvider`); this documents that requirement on
    * the corresponding Tapir endpoints without re-implementing the check itself (the actual
    * enforcement remains the existing, tested http4s `AuthMiddleware`).
    */
  val bearerAuth: EndpointInput[String] = auth.bearer[String]()
}
