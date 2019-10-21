package io.dlinov.auth.dao.hikari

import java.util.UUID

import cats.effect.Effect
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.RoleFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.Role
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.RoleFDao
import io.dlinov.auth.domain.PaginatedResult

class RoleHikariDao[F[_]: Effect](db: DBFApi[F]) extends RoleFDao[F] {

  import HikariDBFApi._
  import RoleHikariDao._

  override def create(
      name: String,
      createdBy: String,
      reactivate: Boolean
  ): F[DaoResponse[Role]] = {
    for {
      xa ← db.transactor
      result ← (if (reactivate) {
                  reactivateInternal(name, createdBy)
                } else {
                  createInternal(name, createdBy)
                }).transact(xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .create($name,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findById(id: UUID): F[DaoResponse[Option[Role]]] = {
    for {
      xa     ← db.transactor
      result ← findByIdInternal(id).transact(xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findById($id): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[DaoResponse[PaginatedResult[Role]]] = {
    for {
      xa     ← db.transactor
      result ← findAllInternal(maybeLimit, maybeOffset).transact(xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findAll: " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def update(id: UUID, name: String, updatedBy: String): F[DaoResponse[Option[Role]]] = {
    for {
      xa ← db.transactor
      result ← (for {
        _         ← updateQuery(id, name, updatedBy).update.run
        maybeRole ← findByIdInternal(id)
      } yield maybeRole).transact(xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .update($id, $name, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def remove(id: UUID, updatedBy: String): F[DaoResponse[Option[Role]]] = {
    for {
      xa ← db.transactor
      result ← (for {
        maybeRole ← findByIdInternal(id)
        _         ← removeQuery(id, updatedBy).update.run
      } yield maybeRole).transact(xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .remove($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  private[hikari] def findByIdInternal(id: UUID): ConnectionIO[Option[Role]] = {
    queryById(id)
      .query[Role]
      .option
  }

  private[hikari] def findByNameInternal(name: String): ConnectionIO[Option[Role]] = {
    queryByName(name)
      .query[Role]
      .option
  }

  private[hikari] def findAllInternal(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): ConnectionIO[PaginatedResult[Role]] = {
    for {
      page ← queryAll(maybeLimit, maybeOffset)
        .query[Role]
        .to[List]
      total ← countAll.query[Int].unique
    } yield PaginatedResult(
      total,
      page,
      maybeLimit.getOrElse(Int.MaxValue),
      maybeOffset.getOrElse(0)
    )
  }

  private[hikari] def fetchByIdInternal(id: UUID): ConnectionIO[Role] = {
    queryById(id)
      .query[Role]
      .unique
  }

  private[hikari] def createInternal(name: String, createdBy: String): ConnectionIO[Role] = {
    val id = UUID.randomUUID()
    for {
      _    ← insertQuery(id, name, createdBy).update.run
      role ← fetchByIdInternal(id)
    } yield role
  }

  private[hikari] def reactivateInternal(name: String, createdBy: String): ConnectionIO[Role] = {
    for {
      maybeExistingRole ← findByNameInternal(name)
      reactivated ← maybeExistingRole.fold {
        createInternal(name, createdBy)
      } { existing ⇒
        for {
          _    ← reactivateQuery(existing.id, name, createdBy).update.run
          role ← fetchByIdInternal(existing.id)
        } yield role
      }
    } yield reactivated
  }
}

object RoleHikariDao {
  import HikariDBFApi._

  val TableName: Fragment = Fragment.const("roles")

  val SelectFromTable: Fragment =
    Fragment.const("SELECT `id`,`name`,`cBy`,`uBy`,`cDate`,`uDate` FROM") ++ TableName

  val SelectCountFromTable: Fragment =
    Fragment.const("SELECT COUNT(id) FROM") ++ TableName

  val InsertIntoTable: Fragment =
    Fragment.const("INSERT INTO ") ++ TableName ++ Fragment.const(
      " (id, name, status, cBy, uBy) VALUES "
    )

  val UpdateTable: Fragment =
    Fragment.const("UPDATE ") ++ TableName ++ Fragment.const(" SET ")

  // val DeleteFromTable: Fragment =
  //   Fragment.const("DELETE FROM ") ++ TableName ++ Fragment.const(" WHERE ")

  def queryById(id: UUID): Fragment = SelectFromTable ++ fr"WHERE id = $id and status = 1;"

  def queryByName(name: String): Fragment =
    SelectFromTable ++ fr"WHERE name = $name and status = 1;"

  def queryAll(maybeLimit: Option[Int], maybeOffset: Option[Int]): Fragment = {
    val limit  = maybeLimit.fold(EmptyFragment)(lmt ⇒ fr"LIMIT $lmt")
    val offset = maybeOffset.fold(EmptyFragment)(off ⇒ fr"OFFSET $off")
    SelectFromTable ++ fr"WHERE status = 1" ++ limit ++ offset
  }

  val countAll: Fragment = SelectCountFromTable ++ fr"WHERE status = 1"

  def insertQuery(id: UUID, name: String, cBy: String): Fragment = {
    InsertIntoTable ++ fr"($id, $name, 1, $cBy, $cBy);"
  }

  def reactivateQuery(id: UUID, name: String, cBy: String): Fragment = {
    UpdateTable ++ fr"name = $name, status = 1, uBy = $cBy WHERE id = $id;"
  }

  def updateQuery(id: UUID, name: String, uBy: String): Fragment = {
    UpdateTable ++ fr"name = $name, uBy = $uBy WHERE id = $id;"
  }

  def removeQuery(id: UUID, uBy: String): Fragment = {
    // DeleteFromTable ++ fr"id = $id;"
    UpdateTable ++ fr"status = 0, uBy = $uBy WHERE id = $id;"
  }
}
