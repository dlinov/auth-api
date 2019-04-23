package io.dlinov.auth.domain.algebras

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.syntax.either._
import io.dlinov.auth.{AppConfig, AuthenticationBehavior, TokenBehavior}
import io.dlinov.auth.domain.{BaseService, ServiceError}
import io.dlinov.auth.domain.ErrorCodes.AccountTemporarilyLocked
import io.dlinov.auth.domain.algebras.services.{CaptchaService, NotificationService}
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, ClaimContent, Email, Notifications, PasswordResetClaim}
import io.dlinov.auth.routes.dto.LoginStatusResponse
import org.http4s.Uri
import scalacache._
import scalacache.caffeine._
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.ErrorCodes.AccountTemporarilyLocked
import io.dlinov.auth.domain.{BaseService, ServiceError}
import io.dlinov.auth.domain.auth.entities._
import io.dlinov.auth.{AppConfig, AuthenticationBehavior, TokenBehavior}
import io.dlinov.auth.routes.json.CirceEncoders.{claimContentEncoder, passwordResetClaimEncoder}
import io.dlinov.auth.domain.algebras.services.{CaptchaService, NotificationService}
import io.dlinov.auth.routes.dto.LoginStatusResponse

import scala.concurrent.duration.Duration

class AuthenticationAlgebra(
    config: AppConfig,
    backOfficeUserAlgebra: BackOfficeUserAlgebra,
    passwordAlgebra: PasswordAlgebra,
    captchaService: CaptchaService,
    notificationService: NotificationService)
  extends BaseService with TokenBehavior with AuthenticationBehavior {
  private implicit val mode: Mode[IO] = scalacache.CatsEffect.modes.async[IO]

  import AuthenticationAlgebra._

  private val authConfig = config.auth
  private val lockDuration = authConfig.accountLockTimeout
  private val maxBadAttempts = authConfig.maxBadLoginAttempts
  private val captchaRequired: Boolean = authConfig.requireCaptcha
  private val appHost = Uri.unsafeFromString(config.app.host)
  private implicit val loginCache: CaffeineCache[Long] = CaffeineCache[Long](CacheConfig.defaultCacheConfig)

  override val tokenExpirationInMinutes: Int = authConfig.tokenExpirationOffsetMinutes

  def login(
    userName: String,
    password: String,
    maybeCaptcha: Option[String]): IO[ServiceResponse[(BackOfficeUser, String)]] = {
    val hashedPassword = hashPassword(password)
    (for {
      loginAttempts ← EitherT(incUserLoginTriesCache(userName))
      _ ← EitherT(checkCaptcha(maybeCaptcha))
      user ← EitherT(backOfficeUserAlgebra.login(userName, hashedPassword))
        .leftMap { err ⇒
          if (loginAttempts == maxBadAttempts && err.code != AccountTemporarilyLocked) {
            buildLockErrorMessage
          } else {
            err
          }
        }
      _ ← EitherT.liftF[IO, ServiceError, Any](cleanUserLoginCache(user.userName))
    } yield {
      val claimContent = ClaimContent.from(user)
      val token = generateTokenCirce(userName, claimContent)
      user → token
    }).value
  }

  def updatePassword(
    userName: String,
    oldPassword: String,
    newPassword: String): IO[ServiceResponse[(BackOfficeUser, String)]] = {
    (for {
      hashes ← EitherT(IO {
        validatePassword(Some(oldPassword), newPassword)
          .bimap(
            errors ⇒ {
              // TODO: find a way to use StringBuilder
              val msg = "[Password Errors] " + errors.reduceLeft(_ + ", " + _)
              validationError(msg)
            },
            _ ⇒ hashPassword(oldPassword) → hashPassword(newPassword))
      })
      (oldPasswordHash, newPasswordHash) = hashes
      user ← EitherT(backOfficeUserAlgebra.updatePassword(userName, oldPasswordHash, newPasswordHash))
      _ ← EitherT.liftF[IO, ServiceError, Any](cleanUserLoginCache(user.userName))
    } yield {
      val claimContent = ClaimContent.from(user)
      val token = generateTokenCirce(userName, claimContent)
      user → token
    }).value
  }

  def sendPasswordResetLink(
    userName: String,
    email: Email,
    maybeReferer: Option[String],
    maybeCaptcha: Option[String]): IO[ServiceResponse[Unit]] = {
    (for {
      _ ← EitherT(checkCaptcha(maybeCaptcha))
      user ← EitherT(backOfficeUserAlgebra.findByName(userName))
      notification ← EitherT(IO {
        if (user.email == email) {
          val resetPasswordToken = generateTokenCirce(userName, PasswordResetClaim(userName))
          val resetPasswordLink = maybeReferer
            .flatMap(r ⇒ Uri.fromString(r).toOption)
            .getOrElse(appHost)
            .resolve(Uri.unsafeFromString(s"/reset_password?token=$resetPasswordToken"))
            .renderString
          val resetPasswordMessage =
            s"""Hello $userName,
               |
               |Click the following link to reset your password: $resetPasswordLink.
               |The link will expire in $tokenExpirationInMinutes minutes.""".stripMargin
          Right(Notifications.textNotification(user, "Reset password request", resetPasswordMessage))
        } else {
          Left(notFoundEntityError(s"There is no such user as $userName/$email"))
        }
      })
      resp ← EitherT(notificationService.sendNotification(notification))
    } yield resp).value
  }

  def validatePasswordResetToken(token: String): IO[ServiceResponse[Unit]] = {
    IO(passwordResetClaim(token).map(_ ⇒ ()))
  }

  def resetPassword(password: String, token: String): IO[ServiceResponse[(BackOfficeUser, String)]] = {
    (for {
      userName ← EitherT(IO(passwordResetClaim(token).map(_.userName)))
      passwordHash ← EitherT(IO {
        validatePassword(None, password)
          .bimap(
            errors ⇒ {
              // TODO: find a way to use StringBuilder
              val msg = "[Password Errors] " + errors.reduceLeft(_ + ", " + _)
              validationError(msg)
            },
            hashPassword)
      })
      user ← EitherT(backOfficeUserAlgebra.resetPassword(userName, passwordHash))
      _ ← EitherT.liftF[IO, ServiceError, Any](cleanUserLoginCache(user.userName))
    } yield {
      val claimContent = ClaimContent.from(user)
      val token = generateTokenCirce(userName, claimContent)
      user → token
    }).value
  }

  def status: IO[ServiceResponse[LoginStatusResponse]] = {
    IO(Right(LoginStatusResponse(requireCaptcha = captchaRequired)))
  }

  def validateToken(token: String): IO[ServiceResponse[BackOfficeUser]] = {
    (for {
      claim ← EitherT(IO(userClaim(token)))
      user ← EitherT(backOfficeUserAlgebra.findByName(claim.loggedInAs))
    } yield user).value
  }

  protected def hashPassword(password: String): String = passwordAlgebra.hashPassword(password)

  protected def validatePassword(
    maybeOldPassword: Option[String],
    password: String): Either[NonEmptyList[String], String] = {
    passwordAlgebra.validatePassword(maybeOldPassword, password)
  }

  protected def checkCaptcha(maybeCaptcha: Option[String]): IO[ServiceResponse[Unit]] = {
    if (captchaRequired) {
      maybeCaptcha.fold[IO[ServiceResponse[Unit]]] {
        IO(Left(ServiceError.captchaRequiredError("`captcha` field is missing")))
      } {
        captchaService.checkCaptcha
      }
    } else {
      IO.pure(Right(()))
    }
  }

  protected def buildLockErrorMessage: ServiceError = {
    val msg = s"You've exceeded max login attempts ($maxBadAttempts). " +
      s"Your account has been locked for ${lockDuration.toMinutes} minutes."
    ServiceError.accountLockedError(msg)
  }

  protected def incUserLoginTriesCache(userName: String): IO[ServiceResponse[Long]] = {
    for {
      cacheKey ← IO(makeLoginCacheKey(userName))
      maybeCachedTries ← get[IO, Long](cacheKey)
      tries = {
        logCacheHitOrMiss(cacheKey, maybeCachedTries)
        maybeCachedTries.getOrElse(0L) + 1
      }
      okOrError ← if (tries > maxBadAttempts) {
        IO(Left(buildLockErrorMessage))
      } else {
        logCachePut(key = cacheKey, Some(lockDuration))
        put(cacheKey)(value = tries, ttl = Some(lockDuration))
          .map(_ ⇒ Right(tries))
      }
    } yield okOrError
  }

  protected def cleanUserLoginCache(userName: String): IO[Any] = {
    remove(makeLoginCacheKey(userName))
  }

  @inline private def makeLoginCacheKey(userName: String): String = userName + CacheLoginTries

  /**
   * Copied from scalacache.LoggingSupport as `logger` from it has type different from `logger` here
   * Output a debug log to record the result of a cache lookup
   *
   * @param key the key that was looked up
   * @param result the result of the cache lookup
   * @tparam A the type of the cache value
   */
  protected def logCacheHitOrMiss[A](key: String, result: Option[A]): Unit = {
    if (logger.isDebugEnabled) {
      val hitOrMiss = result.map(_ ⇒ "hit") getOrElse "miss"
      logger.debug(s"Cache $hitOrMiss for key $key")
    }
  }

  /**
   * Copied from scalacache.LoggingSupport as `logger` from it has type different from `logger` here
   * Output a debug log to record a cache insertion/update
   *
   * @param key the key that was inserted/updated
   * @param ttl the TTL of the inserted entry
   */
  protected def logCachePut(key: String, ttl: Option[Duration]): Unit = {
    if (logger.isDebugEnabled) {
      val ttlMsg = ttl.map(d ⇒ s" with TTL ${d.toMillis} ms") getOrElse ""
      logger.debug(s"Inserted value into cache with key $key$ttlMsg")
    }
  }

}

object AuthenticationAlgebra {
  private final val CacheLoginTries = ".loginTries"
}
