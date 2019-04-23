package io.dlinov.auth.domain.algebras.services

import cats.effect.{IO, Timer}
import cats.syntax.either._
import cats.syntax.apply._
import io.dlinov.auth.domain.auth.entities.Notification
import io.dlinov.auth.util.Logging
import org.apache.commons.mail.{Email, SimpleEmail}
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.AppConfig.EmailConfig
import io.dlinov.auth.util.Logging
import io.dlinov.auth.util.Implicits.Escaper

import scala.concurrent.duration._

class EmailNotificationService(emailConfig: EmailConfig)(implicit timer: Timer[IO])
  extends NotificationService with Logging {

  protected val hostName: String = emailConfig.host
  protected val smtpPort: Int = emailConfig.port
  protected val fromAddress: String = emailConfig.senderAddress
  protected val fromName: String = emailConfig.senderName
  protected val retryTimeout: FiniteDuration = emailConfig.retryTimeout
  protected val maxRetries: Int = emailConfig.maxRetries

  override def sendNotification(notification: Notification): IO[ServiceResponse[Unit]] = {
    retryWithBackoff(sendEmailFlow(notification), maxRetries).attempt
      .map(_.bimap(
        exc ⇒ {
          val errorMsg = s"Failed to send email notification ${notification.id} to ${notification.to.email.value}."
            .escapeJava
          logger.warn(errorMsg, exc)
          unknownError(errorMsg)
        },
        _ ⇒ ()))
  }

  protected def sendEmailFlow(notification: Notification): IO[String] = IO {
    val email = makeEmail(notification)
    email.send()
  }

  protected def retryWithBackoff[A](ioa: IO[A], retriesLeft: Int)(implicit timer: Timer[IO]): IO[A] = {

    ioa.handleErrorWith { error ⇒
      if (retriesLeft > 0)
        IO.sleep(retryTimeout) *> retryWithBackoff(ioa, retriesLeft - 1)
      else
        IO.raiseError(error)
    }
  }

  protected def makeEmail(notification: Notification): Email = {
    val email = new SimpleEmail()
    email.setHostName(hostName)
    email.setSmtpPort(smtpPort)
    val user = notification.to
    val recipientAddress = user.email.value
    val recipientName = s"${user.firstName} ${user.lastName}"
    email.addTo(recipientAddress, recipientName)
    email.setFrom(fromAddress, fromName)
    email.setSubject(notification.topic)
    email.setMsg(notification.message)
    email
  }
}
