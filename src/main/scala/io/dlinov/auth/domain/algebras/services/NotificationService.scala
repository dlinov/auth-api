package io.dlinov.auth.domain.algebras.services

import io.dlinov.auth.domain.BaseService
import io.dlinov.auth.domain.auth.entities.Notification
import io.dlinov.auth.domain.BaseService
import io.dlinov.auth.domain.BaseService.ServiceResponse

trait NotificationService[F[_]] extends BaseService {
  def sendNotification(notification: Notification): F[ServiceResponse[Unit]]
}
