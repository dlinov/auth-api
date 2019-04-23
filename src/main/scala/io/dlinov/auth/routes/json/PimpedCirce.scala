package io.dlinov.auth.routes.json

import java.util.UUID

import cats.Applicative
import cats.data.NonEmptyList
import cats.syntax.applicative._
import cats.syntax.foldable._
import cats.instances.string._
import io.circe.{DecodingFailure, Json}
import io.dlinov.auth.domain.ErrorCodes.ValidationFailed
import io.dlinov.auth.routes.dto.ApiError
import io.dlinov.auth.routes.json.CirceEncoders.apiErrorEncoder
import org.http4s.{DecodeFailure, EntityEncoder, HttpVersion, MessageBodyFailure, Response, Status}
import org.http4s.circe.{CirceInstances, DecodingFailures}

object PimpedCirce extends CirceInstances {
  implicit def apiErrorEntityEncoder[F[_]: Applicative]: EntityEncoder[F, ApiError] = jsonEncoderOf[F, ApiError]

  final case class PimpedInvalidMessageBodyFailure(details: String, cause: Option[Throwable] = None)
    extends MessageBodyFailure {
    def message: String = details

    override def toHttpResponse[F[_]: Applicative](httpVersion: HttpVersion): F[Response[F]] = {
      // TODO: pass id and params
      val apiError = ApiError(UUID.randomUUID(), ValidationFailed, message, None)
      Response[F](Status.UnprocessableEntity, httpVersion)
        .withEntity(apiError)(apiErrorEntityEncoder)
        .pure[F]
    }
  }

  override protected def jsonDecodeError: (Json, NonEmptyList[DecodingFailure]) ⇒ DecodeFailure = (_, failures) ⇒ {
    PimpedInvalidMessageBodyFailure(
      failures.map(_.message).mkString_("Errors:\n", "\n", ""),
      if (failures.tail.isEmpty) Some(failures.head) else Some(DecodingFailures(failures)))
  }

}
