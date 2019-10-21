package io.dlinov.auth.routes

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.Monad
import cats.data.{NonEmptyList, Validated}
import cats.effect.Sync
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
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.TokenExtractBehavior
import io.dlinov.auth.routes.json.CirceEncoders.apiErrorEncoder
import io.dlinov.auth.routes.dto.ApiError
import io.dlinov.auth.util.Logging
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Part
import org.http4s.rho.RhoRoutes
import org.http4s.rho.bits.{
  FailureResponse,
  ResultResponse,
  StringParser,
  SuccessResponse,
  TypedQuery
}
import shapeless.{::, HNil}

trait Routes[F[_]] extends TokenExtractBehavior[F] with Http4sDsl[F] with Logging {
  implicit protected def syncF: Sync[F]

  protected val rhoExt: Routes.RhoExt[F] = Routes.buildRhoExt[F]

  def routes: RhoRoutes[F]

  def handleServiceResponse[T](
      r: ServiceResponse[T]
  )(implicit encoder: Encoder[T]): F[Response[F]] = {
    r.fold(err ⇒ handleError(err), success ⇒ {
      val value: F[Response[F]] = Ok(success.asJson)
      value
    })
  }

  def handleServiceBinaryResponse(r: ServiceResponse[Array[Byte]]): F[Response[F]] = {
    r.fold(err ⇒ handleError(err), success ⇒ Ok(success))
  }

  def handleServiceCreateResponse[T](
      r: ServiceResponse[T]
  )(implicit encoder: Encoder[T]): F[Response[F]] = {
    r.fold(err ⇒ handleError(err), success ⇒ Created(success.asJson))
  }

  def handleError(error: ServiceError): F[Response[F]] = {
    logger.warn(error.toString)
    val id      = error.id
    val code    = error.code
    val message = error.message
    val maybeParams = for {
      f ← error.fieldName
      v ← error.fieldValue
    } yield ApiError.ErrorParams(f, v)
    val responseBody = ApiError(id, code, message, maybeParams).asJson
    error.code match {
      case ValidationFailed | DuplicateEntity | InvalidCaptcha ⇒
        BadRequest(responseBody)
      case NotAuthorized | CaptchaRequired ⇒
        val authenticate =
          headers.`WWW-Authenticate`(NonEmptyList.of(Challenge("Bearer", "API access denied")))
        Unauthorized(authenticate, responseBody)
      case PermissionsInsufficient ⇒
        Forbidden(responseBody)
      case AccountTemporarilyLocked ⇒
        TooManyRequests(responseBody)
      case NotFoundEntity ⇒
        NotFound(responseBody)
      case Unknown | InvalidConfig ⇒
        InternalServerError(responseBody)
    }
  }
}

object Routes {
  // not too reliable import, tried to minimize its impact
  import org.http4s.dsl.impl._

  val ApiPrefix  = "api"
  val ApiRoot: / = Root / ApiPrefix

  implicit private val uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    (queryParamValue: QueryParameterValue) ⇒
      Validated
        .catchNonFatal(UUID.fromString(queryParamValue.value))
        .leftMap(t ⇒ ParseFailure(s"Query decoding of UUID failed", t.getMessage))
        .toValidatedNel

  implicit private val localDateQueryParamDecoder: QueryParamDecoder[ZonedDateTime] =
    (param: QueryParameterValue) ⇒
      Validated
        .catchNonFatal(LocalDate.parse(param.value).atStartOfDay(ZoneOffset.UTC))
        .leftMap(t ⇒ ParseFailure(s"Query decoding of LocalDate failed", t.getMessage))
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

  object TokenQParam  extends QueryParamDecoderMatcher[String]("token")
  object BUIdQParam   extends QueryParamDecoderMatcher[UUID]("business_unit_id")
  object RoleIdQParam extends QueryParamDecoderMatcher[UUID]("role_id")

  def buildRhoExt[F[_]](implicit m: Monad[F]): RhoExt[F] = new RhoExt[F]

  implicit private[this] def zdtStringParser[F[_]]: StringParser[F, ZonedDateTime] = {
    new StringParser[F, ZonedDateTime] {
      import scala.reflect.runtime.universe._
      import scala.util.control.NonFatal

      override val typeTag: Option[TypeTag[ZonedDateTime]] = Some(
        implicitly[TypeTag[ZonedDateTime]]
      )

      override def parse(s: String)(implicit m: Monad[F]): ResultResponse[F, ZonedDateTime] = {
        try {
          SuccessResponse(LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC))
        } catch {
          case NonFatal(_) ⇒
            FailureResponse.pure[F] {
              BadRequest.pure(
                s"Invalid instant format, should be in 'yyyy-MM-ddThh:mm:ssZ' format: $s"
              )
            }
        }
      }
    }
  }

  implicit final class MultipartOps[F[_]: Sync](val part: Part[F]) {
    def extractBytes: F[Vector[Byte]] =
      part.body.compile.toVector

    def extractString: F[String] =
      part.body
        .through(fs2.text.utf8Decode)
        .compile
        .toVector
        .map(_.mkString("\n"))
  }

  implicit class DecodeFailureOps(val df: DecodeFailure) extends AnyVal {
    def asServiceError: ServiceError = {
      val details = (for {
        c     ← df.cause
        cause ← Option(c.asInstanceOf[DecodingFailure])
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

  protected class RhoExt[F[_]: Monad] {
    private val rhoF = new RhoDsl[F] {}
    import rhoF._

    val reactivateParam: TypedQuery[F, Option[Boolean] :: HNil] =
      param[Option[Boolean]]("reactivate")
    val limitAndOffsetParams: TypedQuery[F, Option[Int] :: Option[Int] :: HNil] =
      param[Option[Int]]("limit") & param[Option[Int]]("offset")
    val userIdParam: TypedQuery[F, Option[UUID] :: HNil]     = param[Option[UUID]]("user_id")
    val customerIdParam: TypedQuery[F, Option[UUID] :: HNil] = param[Option[UUID]]("customer_id")
    val statusParam: TypedQuery[F, Option[String] :: HNil]   = param[Option[String]]("status")
    val startDateParam: TypedQuery[F, Option[ZonedDateTime] :: HNil] =
      param[Option[ZonedDateTime]]("start_date")
    val endDateParam: TypedQuery[F, Option[ZonedDateTime] :: HNil] =
      param[Option[ZonedDateTime]]("end_date")
    val sortByParam: TypedQuery[F, Option[String] :: HNil] = param[Option[String]]("sort_by")
    val buIdParam: TypedQuery[F, UUID :: HNil]             = param[UUID]("business_unit_id")
    val roleIdParam: TypedQuery[F, UUID :: HNil]           = param[UUID]("role_id")

    val firstNameParam: TypedQuery[F, Option[String] :: HNil] = param[Option[String]]("first_name")
    val lastNameParam: TypedQuery[F, Option[String] :: HNil]  = param[Option[String]]("last_name")
    val emailParam: TypedQuery[F, Option[String] :: HNil]     = param[Option[String]]("email")
    val phoneNumberParam: TypedQuery[F, Option[String] :: HNil] =
      param[Option[String]]("phone_number")
    val bouSearchParams: TypedQuery[F, Option[String] :: Option[String] :: Option[String] :: Option[
      String
    ] :: HNil] =
      firstNameParam & lastNameParam & emailParam & phoneNumberParam
  }

}
