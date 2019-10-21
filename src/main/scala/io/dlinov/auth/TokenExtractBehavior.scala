package io.dlinov.auth

import java.util.UUID

import cats.syntax.either._
import io.dlinov.auth.domain.ServiceError
import org.http4s.Request
import org.http4s.headers.Authorization
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.ServiceError

trait TokenExtractBehavior[F[_]] {
  protected def extractTokenFromRequest(request: Request[F]): ServiceResponse[String] = {
    Authorization
      .from(request.headers)
      .fold[ServiceResponse[String]] {
        Left(ServiceError.notAuthorizedError(UUID.randomUUID(), "Authorization header is missing"))
      }(extractTokenFromHeader)
  }

  protected def extractTokenFromHeader(header: Authorization.HeaderT): ServiceResponse[String] = {
    (for {
      rawToken ← {
        val hValue = header.value
        val i      = hValue.indexOf("Bearer ")
        if (i > -1) {
          Right(hValue.substring(i + "Bearer ".length))
        } else {
          Left("Bearer token required")
        }
      }
    } yield rawToken).leftMap(ServiceError.notAuthorizedError(UUID.randomUUID(), _))
  }
}
