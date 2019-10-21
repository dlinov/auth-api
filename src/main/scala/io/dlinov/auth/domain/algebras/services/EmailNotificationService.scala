package io.dlinov.auth.domain.algebras.services

import cats.ApplicativeError
import cats.effect.{ConcurrentEffect, Sync, Timer}
import cats.syntax.either._
import cats.syntax.apply._
import cats.syntax.functor._
import io.dlinov.auth.domain.auth.entities.Notification
import io.dlinov.auth.util.Logging
import org.apache.commons.mail.{Email, SimpleEmail}
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.AppConfig.EmailConfig
import io.dlinov.auth.util.Logging
import io.dlinov.auth.util.Implicits.Escaper

import scala.concurrent.duration._

class EmailNotificationService[F[_]: ConcurrentEffect: Timer](emailConfig: EmailConfig)
    extends NotificationService[F]
    with Logging {

  protected val hostName: String             = emailConfig.host
  protected val smtpPort: Int                = emailConfig.port
  protected val fromAddress: String          = emailConfig.senderAddress
  protected val fromName: String             = emailConfig.senderName
  protected val retryTimeout: FiniteDuration = emailConfig.retryTimeout
  protected val maxRetries: Int              = emailConfig.maxRetries

  override def sendNotification(notification: Notification): F[ServiceResponse[Unit]] = {
    ConcurrentEffect[F]
      .attempt(retryWithBackoff(sendEmailFlow(notification), maxRetries))
      .map(
        _.bimap(
          exc ⇒ {
            val errorMsg =
              s"Failed to send email notification ${notification.id} to ${notification.to.email.value}.".escapeJava
            logger.warn(errorMsg, exc)
            unknownError(errorMsg)
          },
          _ ⇒ ()
        )
      )
  }

  protected def sendEmailFlow(notification: Notification): F[String] = Sync[F].delay {
    val email = makeEmail(notification)
    email.send()
  }

  protected def retryWithBackoff[A](ioa: F[A], retriesLeft: Int): F[A] = {
    val ae = ApplicativeError[F, Throwable]
    ae.handleErrorWith(ioa) { error ⇒
      if (retriesLeft > 0)
        Timer[F].sleep(retryTimeout) *> retryWithBackoff(ioa, retriesLeft - 1)
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
