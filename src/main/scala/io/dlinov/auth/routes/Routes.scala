package io.dlinov.auth.routes

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.data.{NonEmptyList, Validated}
import cats.effect.{Async, Sync}
import cats.syntax.functor._
import cats.syntax.show._
import io.circe.CursorOp.showCursorOp
import io.circe.{DecodingFailure, Encoder}
import io.circe.syntax._
import io.dlinov.auth.TokenExtractBehavior
import io.dlinov.auth.domain.ErrorCodes.{
  AccountTemporarilyLocked,
  CaptchaRequired,
  DuplicateEntity,
  InvalidCaptcha,
  InvalidConfig,
  NotAuthorized,
  NotFoundEntity,
  PermissionsInsufficient,
  Unknown,
  ValidationFailed
}
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.routes.dto.ApiError
import io.dlinov.auth.util.Logging
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.routes.json.CirceEncoders.apiErrorEncoder
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Part

trait Routes[F[_]] extends TokenExtractBehavior[F] with Http4sDsl[F] with Logging {
  // typed as Async[F] (rather than the narrower Sync[F]) so that traits mixed alongside Routes,
  // such as EntityDecoders, which need Concurrent[F] for http4s-circe's `jsonOf`, can reuse it.
  implicit protected def asyncF: Async[F]

  def handleServiceResponse[T](
      r: ServiceResponse[T]
  )(implicit encoder: Encoder[T]): F[Response[F]] = {
    r.fold(
      err => handleError(err),
      success => {
        val value: F[Response[F]] = Ok(success.asJson)
        value
      }
    )
  }

  def handleServiceBinaryResponse(r: ServiceResponse[Array[Byte]]): F[Response[F]] = {
    r.fold(err => handleError(err), success => Ok(success))
  }

  def handleServiceCreateResponse[T](
      r: ServiceResponse[T]
  )(implicit encoder: Encoder[T]): F[Response[F]] = {
    r.fold(err => handleError(err), success => Created(success.asJson))
  }

  def handleError(error: ServiceError): F[Response[F]] = {
    logger.warn(error.toString)
    val id          = error.id
    val code        = error.code
    val message     = error.message
    val maybeParams = for {
      f <- error.fieldName
      v <- error.fieldValue
    } yield ApiError.ErrorParams(f, v)
    val responseBody = ApiError(id, code, message, maybeParams).asJson
    error.code match {
      case ValidationFailed | DuplicateEntity | InvalidCaptcha =>
        BadRequest(responseBody)
      case NotAuthorized | CaptchaRequired =>
        val authenticate =
          headers.`WWW-Authenticate`(NonEmptyList.of(Challenge("Bearer", "API access denied")))
        Unauthorized(authenticate, responseBody)
      case PermissionsInsufficient =>
        Forbidden(responseBody)
      case AccountTemporarilyLocked =>
        TooManyRequests(responseBody)
      case NotFoundEntity =>
        NotFound(responseBody)
      case Unknown | InvalidConfig =>
        InternalServerError(responseBody)
    }
  }
}

object Routes {
  // not too reliable import, tried to minimize its impact
  import org.http4s.dsl.impl._

  val ApiPrefix         = "api"
  val ApiRoot: Uri.Path = Uri.Path.Root / ApiPrefix

  implicit private val uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    (queryParamValue: QueryParameterValue) =>
      Validated
        .catchNonFatal(UUID.fromString(queryParamValue.value))
        .leftMap(t => ParseFailure(s"Query decoding of UUID failed", t.getMessage))
        .toValidatedNel

  implicit private val localDateQueryParamDecoder: QueryParamDecoder[ZonedDateTime] =
    (param: QueryParameterValue) =>
      Validated
        .catchNonFatal(LocalDate.parse(param.value).atStartOfDay(ZoneOffset.UTC))
        .leftMap(t => ParseFailure(s"Query decoding of LocalDate failed", t.getMessage))
        .toValidatedNel

  object MaybeReactivateQParam extends OptionalQueryParamDecoderMatcher[Boolean]("reactivate")
  object MaybeUserIdQParam     extends OptionalQueryParamDecoderMatcher[UUID]("user_id")
  object MaybeCustomerIdQParam extends OptionalQueryParamDecoderMatcher[UUID]("customer_id")
  object MaybeStatusQParam     extends OptionalQueryParamDecoderMatcher[String]("status")
  object MaybeStartDateQParam  extends OptionalQueryParamDecoderMatcher[ZonedDateTime]("start_date")
  object MaybeEndDateQParam    extends OptionalQueryParamDecoderMatcher[ZonedDateTime]("end_date")
  object MaybeSortByQParam     extends OptionalQueryParamDecoderMatcher[String]("sort_by")
  object MaybeOffsetQParam     extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object MaybeLimitQParam      extends OptionalQueryParamDecoderMatcher[Int]("limit")

  object MaybeFirstNameQParam   extends OptionalQueryParamDecoderMatcher[String]("first_name")
  object MaybeLastNameQParam    extends OptionalQueryParamDecoderMatcher[String]("last_name")
  object MaybeEmailQParam       extends OptionalQueryParamDecoderMatcher[String]("email")
  object MaybePhoneNumberQParam extends OptionalQueryParamDecoderMatcher[String]("phone_number")

  object TokenQParam  extends QueryParamDecoderMatcher[String]("token")
  object BUIdQParam   extends QueryParamDecoderMatcher[UUID]("business_unit_id")
  object RoleIdQParam extends QueryParamDecoderMatcher[UUID]("role_id")

  /** Checks whether `path` is the given `root` path (expressed as a plain, slash-separated string
    * such as `"api/accounts"`) or one of its descendants (e.g. `"api/accounts/123"`).
    *
    * Segment-wise comparison is used rather than raw string prefix matching, so that a sibling path
    * sharing only a textual prefix (e.g. `"api/accountsXYZ"` against root `"api/accounts"`) does
    * NOT match, while true descendants of `root` still do.
    */
  def isPathUnderRoot(root: String)(path: Uri.Path): Boolean = {
    val rootSegments = root.split("/").toIndexedSeq.filter(_.nonEmpty)
    val pathSegments = path.segments.map(_.decoded())
    pathSegments.startsWith(rootSegments)
  }

  implicit final class MultipartOps[F[_]: Sync](val part: Part[F]) {
    def extractBytes: F[Vector[Byte]] =
      part.body.compile.toVector

    def extractString: F[String] =
      part.body
        .through(fs2.text.utf8.decode)
        .compile
        .toVector
        .map(_.mkString("\n"))
  }

  implicit class DecodeFailureOps(val df: DecodeFailure) extends AnyVal {
    def asServiceError: ServiceError = {
      val details = (for {
        c     <- df.cause
        cause <- Option(c.asInstanceOf[DecodingFailure])
      } yield {
        if (cause.history.isEmpty) {
          cause.message
        } else {
          s"invalid fields: ${cause.history.map(_.show).mkString(",")}"
        }
      }).getOrElse("")
      val error = ServiceError.validationError(UUID.randomUUID(), s"${df.getMessage()}: $details")
      error
    }
  }

}
