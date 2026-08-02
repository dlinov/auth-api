package io.dlinov.auth.domain.algebras.services

import java.io.IOException
import java.net.{ConnectException, SocketException, SocketTimeoutException}

import cats.ApplicativeError
import cats.effect.Async
import cats.syntax.either._
import cats.syntax.apply._
import cats.syntax.functor._
import io.dlinov.auth.domain.auth.entities.Notification
import io.dlinov.auth.util.Logging
import org.apache.commons.mail.{Email, SimpleEmail}
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.AppConfig.EmailConfig
import io.dlinov.auth.util.Implicits.Escaper

import scala.annotation.tailrec
import scala.concurrent.duration._

class EmailNotificationService[F[_]: Async](emailConfig: EmailConfig)
    extends NotificationService[F]
    with Logging {

  protected val hostName: String             = emailConfig.host
  protected val smtpPort: Int                = emailConfig.port
  protected val fromAddress: String          = emailConfig.senderAddress
  protected val fromName: String             = emailConfig.senderName
  protected val retryTimeout: FiniteDuration = emailConfig.retryTimeout
  protected val maxRetries: Int              = emailConfig.maxRetries

  override def sendNotification(notification: Notification): F[ServiceResponse[Unit]] = {
    Async[F]
      .attempt(retryWithBackoff(sendEmailFlow(notification), maxRetries))
      .map(
        _.bimap(
          exc => {
            val errorMsg =
              s"Failed to send email notification ${notification.id} to ${notification.to.email.value}.".escapeJava
            logger.warn(errorMsg, exc)
            unknownError(errorMsg)
          },
          _ => ()
        )
      )
  }

  // SMTP sending is a blocking, synchronous JavaMail call: `Async[F].blocking` (rather than
  // `Sync[F].delay`) shifts it onto a dedicated blocking-friendly execution context, so it
  // cannot starve the F runtime's compute pool the way a `delay`-wrapped blocking call would.
  protected def sendEmailFlow(notification: Notification): F[String] = Async[F].blocking {
    val email = makeEmail(notification)
    email.send()
  }

  protected def retryWithBackoff[A](ioa: F[A], retriesLeft: Int): F[A] = {
    val ae = ApplicativeError[F, Throwable]
    ae.handleErrorWith(ioa) { error =>
      if (retriesLeft > 0 && EmailNotificationService.isTransientFailure(error))
        Async[F].sleep(retryTimeout) *> retryWithBackoff(ioa, retriesLeft - 1)
      else
        ae.raiseError(error)
    }
  }

  protected def makeEmail(notification: Notification): Email = {
    val email = new SimpleEmail()
    email.setHostName(hostName)
    email.setSmtpPort(smtpPort)
    val user             = notification.to
    val recipientAddress = user.email.value
    val recipientName    = s"${user.firstName} ${user.lastName}"
    email.addTo(recipientAddress, recipientName)
    email.setFrom(fromAddress, fromName)
    email.setSubject(notification.topic)
    email.setMsg(notification.message)
    email
  }
}

object EmailNotificationService {

  /** Retries are only warranted for transient network/connectivity failures (timeouts, refused or
    * reset connections, generic I/O errors while talking to the SMTP relay). Permanent failures -
    * bad credentials, an invalid/rejected recipient, malformed message - would just fail
    * identically on every retry and shouldn't burn through `maxRetries` before reporting the
    * (unchanged) explicit error.
    */
  @tailrec
  private[services] def isTransientFailure(t: Throwable): Boolean = t match {
    case null                             => false
    case _: SocketTimeoutException        => true
    case _: ConnectException              => true
    case _: SocketException               => true
    case _: IOException                   => true
    case other if other.getCause ne other => isTransientFailure(other.getCause)
    case _                                => false
  }
}
