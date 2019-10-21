package io.dlinov.auth.domain.auth.entities

object Notifications {
  final private case class TextNotification(to: BackOfficeUser, topic: String, message: String)
      extends Notification

  def textNotification(to: BackOfficeUser, topic: String, message: String): Notification = {
    TextNotification(to, topic, message)
  }
}
