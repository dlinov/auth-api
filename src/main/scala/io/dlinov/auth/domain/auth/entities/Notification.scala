package io.dlinov.auth.domain.auth.entities

import java.util.UUID

trait Notification {
  val id: UUID = UUID.randomUUID()
  def to: BackOfficeUser
  def topic: String
  def message: String
}
