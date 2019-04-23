package io.dlinov.auth.domain.algebras.services

import cats.effect.IO
import io.dlinov.auth.domain.BaseService
import io.dlinov.auth.domain.auth.entities.Notification
import io.dlinov.auth.domain.BaseService
import io.dlinov.auth.domain.BaseService.ServiceResponse

trait NotificationService extends BaseService {
  def sendNotification(notification: Notification): IO[ServiceResponse[Unit]]
}
