package io.dlinov.auth.dao

import java.util.UUID

sealed trait DaoError {
  val id: UUID = UUID.randomUUID()
  val message: String

  override def toString: String = s"$message ($id)"
}

object DaoError {
  final case class GenericDbError(message: String) extends DaoError
  final case class ConstraintViolationError(message: String) extends DaoError
  final case class EntityAlreadyExistsError(message: String, field: String, value: String) extends DaoError
  final case class EntityNotFoundError(message: String) extends DaoError
  final case class WrongCredentials(message: String) extends DaoError
}
