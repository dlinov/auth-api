package io.dlinov.auth

import java.util.UUID

import io.dlinov.auth.domain.ServiceError
import org.http4s.Request
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials}
import io.dlinov.auth.domain.BaseService.ServiceResponse

trait TokenExtractBehavior[F[_]] {
  protected def extractTokenFromRequest(request: Request[F]): ServiceResponse[String] = {
    request.headers
      .get[Authorization]
      .fold[ServiceResponse[String]] {
        Left(ServiceError.notAuthorizedError(UUID.randomUUID(), "Authorization header is missing"))
      }(extractTokenFromHeader)
  }

  protected def extractTokenFromHeader(header: Authorization): ServiceResponse[String] = {
    header.credentials match {
      case Credentials.Token(AuthScheme.Bearer, token) => Right(token)
      case _ => Left(ServiceError.notAuthorizedError(UUID.randomUUID(), "****** required"))
    }
  }
}
