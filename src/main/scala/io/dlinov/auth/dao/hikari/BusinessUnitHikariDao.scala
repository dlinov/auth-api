package io.dlinov.auth.dao.hikari

import java.util.UUID

import cats.effect.Effect
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.BusinessUnitFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.BusinessUnit
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.BusinessUnitFDao
import io.dlinov.auth.domain.PaginatedResult

class BusinessUnitHikariDao[F[_]: Effect](db: DBFApi[F]) extends BusinessUnitFDao[F] {

  import HikariDBFApi._
  import BusinessUnitHikariDao._

  override def create(
      name: String,
      createdBy: String,
      reactivate: Boolean
  ): F[DaoResponse[BusinessUnit]] = {
    for {
      xa ← db.transactor
      result ← (if (reactivate) {
                  reactivateInternal(name, createdBy)
                } else {
                  createInternal(name, createdBy)
                }).transact[F](xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .create($name,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findById(id: UUID): F[DaoResponse[Option[BusinessUnit]]] = {
    for {
      xa     ← db.transactor
      result ← findByIdInternal(id).transact[F](xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findById($id): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findAll(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[DaoResponse[PaginatedResult[BusinessUnit]]] = {
    for {
      xa     ← db.transactor
      result ← findAllInternal(maybeLimit, maybeOffset).transact[F](xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findAll: " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def update(
      id: UUID,
      name: String,
      updatedBy: String
  ): F[DaoResponse[Option[BusinessUnit]]] = {
    for {
      xa ← db.transactor
      result ← (for {
        _                 ← updateQuery(id, name, updatedBy).update.run
        maybeBusinessUnit ← findByIdInternal(id)
      } yield maybeBusinessUnit).transact[F](xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .update($id, $name, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def remove(id: UUID, updatedBy: String): F[DaoResponse[Option[BusinessUnit]]] = {
    for {
      xa ← db.transactor
      result ← (for {
        maybeBusinessUnit ← findByIdInternal(id)
        _                 ← removeQuery(id, updatedBy).update.run
      } yield maybeBusinessUnit).transact[F](xa).attemptSql
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .remove($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  private[hikari] def findByIdInternal(id: UUID): ConnectionIO[Option[BusinessUnit]] = {
    queryById(id)
      .query[BusinessUnit]
      .option
  }

  private[hikari] def findByNameInternal(name: String): ConnectionIO[Option[BusinessUnit]] = {
    queryByName(name)
      .query[BusinessUnit]
      .option
  }

  private[hikari] def findAllInternal(
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): ConnectionIO[PaginatedResult[BusinessUnit]] = {
    for {
      page ← queryAll(maybeLimit, maybeOffset)
        .query[BusinessUnit]
        .to[List]
      total ← countAll.query[Int].unique
    } yield PaginatedResult(
      total,
      page,
      maybeLimit.getOrElse(Int.MaxValue),
      maybeOffset.getOrElse(0)
    )

  }

  private[hikari] def fetchByIdInternal(id: UUID): ConnectionIO[BusinessUnit] = {
    queryById(id)
      .query[BusinessUnit]
      .unique
  }

  private[hikari] def createInternal(
      name: String,
      createdBy: String
  ): ConnectionIO[BusinessUnit] = {
    val id = UUID.randomUUID()
    for {
      _  ← insertQuery(id, name, createdBy).update.run
      bu ← fetchByIdInternal(id)
    } yield bu
  }

  private[hikari] def reactivateInternal(
      name: String,
      createdBy: String
  ): ConnectionIO[BusinessUnit] = {
    for {
      maybeExistingBusinessUnit ← findByNameInternal(name)
      reactivated ← maybeExistingBusinessUnit.fold {
        createInternal(name, createdBy)
      } { existing ⇒
        for {
          _  ← reactivateQuery(existing.id, name, createdBy).update.run
          bu ← fetchByIdInternal(existing.id)
        } yield bu
      }
    } yield reactivated
  }
}

object BusinessUnitHikariDao {
  import HikariDBFApi._

  val TableName: Fragment = Fragment.const("business_units")

  val SelectFromTable: Fragment =
    Fragment.const("SELECT `id`,`name`,`cBy`,`uBy`,`cDate`,`uDate` FROM") ++ TableName

  val SelectCountFromTable: Fragment = Fragment.const("SELECT COUNT(id) FROM") ++ TableName

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
