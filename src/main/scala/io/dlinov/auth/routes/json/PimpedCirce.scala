package io.dlinov.auth.routes.json

import java.nio.charset.StandardCharsets
import java.util.UUID

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.instances.string._
import io.circe.syntax._
import io.circe.{DecodingFailure, Json}
import io.dlinov.auth.domain.ErrorCodes.ValidationFailed
import io.dlinov.auth.routes.dto.ApiError
import io.dlinov.auth.routes.json.CirceEncoders.apiErrorEncoder
import org.http4s.{DecodeFailure, EntityEncoder, HttpVersion, MessageBodyFailure, Response, Status}
import org.http4s.circe.{CirceInstances, DecodingFailures}

object PimpedCirce extends CirceInstances {
  implicit def apiErrorEntityEncoder[F[_]]: EntityEncoder[F, ApiError] =
    jsonEncoderOf[F, ApiError]

  final case class PimpedInvalidMessageBodyFailure(details: String, cause: Option[Throwable] = None)
      extends MessageBodyFailure {
    override def message: String = details

    // MessageFailure#toHttpResponse is a pure (non-effectful, single type-param) method in
    // http4s 0.23+, unlike the older effectful `inHttpResponse[F[_], G[_]]`; build the JSON
    // response body directly rather than through the Applicative-constrained EntityEncoder.
    override def toHttpResponse[F[_]](httpVersion: HttpVersion): Response[F] = {
      // TODO: pass id and params
      val apiError = ApiError(UUID.randomUUID(), ValidationFailed, message, None)
      val bytes    = apiError.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
      Response[F](Status.UnprocessableContent, httpVersion)
        .withEntity(bytes)(using EntityEncoder.byteArrayEncoder[F])
        .withContentType(org.http4s.headers.`Content-Type`(org.http4s.MediaType.application.json))
    }
  }

  override protected def jsonDecodeError: (Json, NonEmptyList[DecodingFailure]) => DecodeFailure =
    (_, failures) => {
      PimpedInvalidMessageBodyFailure(
        failures.map(_.message).mkString_("Errors:\n", "\n", ""),
        if (failures.tail.isEmpty) Some(failures.head) else Some(DecodingFailures(failures))
      )
    }

}
