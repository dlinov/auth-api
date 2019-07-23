package io.dlinov.auth

import java.util.UUID

import cats.syntax.either._
import io.circe.parser._
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.{ClaimContent, PasswordResetClaim}
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import io.dlinov.auth.domain.ServiceError
import io.dlinov.auth.domain.auth.entities.{ClaimContent, PasswordResetClaim}
import io.dlinov.auth.routes.json.CirceDecoders.{claimContentDecoder, passwordResetClaimDecoder}

import scala.util.Try

// TODO: probably is worth merging into TokenBehavior
trait AuthenticationBehavior {
  import AuthenticationBehavior._

  protected def notAuthorized: String ⇒ ServiceError = ServiceError.notAuthorizedError(UUID.randomUUID(), _)

  protected def decodeClaim(rawToken: String): Try[JwtClaim] = {
    JwtCirce.decode(rawToken, publicKey, algorithms)
  }

  protected def userClaim(rawToken: String): Either[ServiceError, ClaimContent] = {
    for {
      // .decode takes care about expiration and signature validation
      claim ← decodeClaim(rawToken).toEither
        .leftMap(e ⇒ notAuthorized(e.getMessage))
      jsonContent ← parse(claim.content).leftMap(f ⇒ notAuthorized(f.message))
      userClaim ← jsonContent.as[ClaimContent]
        .leftMap(e ⇒ notAuthorized(e.getMessage))
    } yield userClaim
  }

  protected def passwordResetClaim(rawToken: String): Either[ServiceError, PasswordResetClaim] = {
    for {
      // .decode takes care about expiration and signature validation
      claim ← decodeClaim(rawToken).toEither
        .leftMap(e ⇒ notAuthorized(e.getMessage))
      jsonContent ← parse(claim.content)
        .leftMap(e ⇒ notAuthorized(e.getMessage))
      passwordResetClaim ← jsonContent.as[PasswordResetClaim]
        .leftMap(e ⇒ notAuthorized(e.getMessage))
    } yield passwordResetClaim
  }
}

object AuthenticationBehavior {
  private val publicKey: String = scala.io.Source.fromResource("auth.key.pub").mkString
  private val algorithms: Seq[JwtAsymmetricAlgorithm] = Seq(JwtAlgorithm.ES256)
}
