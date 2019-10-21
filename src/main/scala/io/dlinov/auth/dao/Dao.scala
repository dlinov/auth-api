package io.dlinov.auth.dao

import java.sql.SQLException
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

import org.slf4j.{Logger, LoggerFactory}
import DaoError._

import scala.util.Either

trait Dao {

  protected lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  protected def genericDbError(msg: String) = GenericDbError(msg)

  protected def constraintViolationError(msg: String) = ConstraintViolationError(msg)

  protected def entityAlreadyExistsError(msg: String, field: String, value: String) =
    EntityAlreadyExistsError(msg, field, value)

  protected def entityNotFoundError(msg: String) = EntityNotFoundError(msg)

  protected def wrongCredsError(msg: String) = WrongCredentials(msg)

  def nowUTC: ZonedDateTime = Dao.nowUTC
}

object Dao {
  type DaoResponse[T] = Either[DaoError, T]
  type EntityId       = UUID

  val tz: ZoneOffset = ZoneOffset.UTC

  def nowUTC: ZonedDateTime = ZonedDateTime.now.withZoneSameInstant(tz)

  abstract class DaoException(val cause: Throwable) extends RuntimeException(cause)

  class EntityNotFoundException(val entityId: EntityId, override val cause: Throwable)
      extends DaoException(cause)

  class UniqueConstraintViolationException(val entityId: EntityId, override val cause: SQLException)
      extends DaoException(cause)
}
