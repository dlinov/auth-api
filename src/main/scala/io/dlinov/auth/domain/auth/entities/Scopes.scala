package io.dlinov.auth.domain.auth.entities

class Scopes(val parent: String) {
  val create: String = s"${parent}_create"

  val detail: String = s"${parent}_detail"

  val update: String = s"${parent}_edit"

  val delete: String = s"${parent}_delete"

  def byType: Scopes.Type ⇒ String = {
    case Scopes.Parent ⇒ parent
    case Scopes.Create ⇒ create
    case Scopes.Detail ⇒ detail
    case Scopes.Update ⇒ update
    case Scopes.Delete ⇒ delete
  }
}

object Scopes {
  sealed trait Type

  case object Parent extends Type

  case object Create extends Type

  case object Detail extends Type

  case object Update extends Type

  case object Delete extends Type
}
