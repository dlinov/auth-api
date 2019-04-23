package io.dlinov.auth.dao.hikari

import java.time.ZonedDateTime
import java.util.UUID

import cats.data.OptionT
import doobie._
import doobie.implicits._
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.ScopeFDao
import cats.effect.IO
import cats.syntax.either._
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.ScopeFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Scope
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Scope

class ScopeHikariDao(db: DBFApi[IO])
  extends ScopeFDao {

  import HikariDBFApi._
  import ScopeHikariDao._

  override def create(
    name: String,
    parentId: Option[UUID],
    description: Option[String],
    createdBy: String,
    reactivate: Boolean): IO[DaoResponse[Scope]] = {
    for {
      xa ← db.transactor
      result ← (if (reactivate) {
        reactivateInternal(parentId, name, description, createdBy)
      } else {
        createInternal(parentId, name, description, createdBy)
      }).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .create($name,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findById(id: UUID): IO[DaoResponse[Option[Scope]]] = {
    for {
      xa ← db.transactor
      result ← findByIdInternal(id).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findById($id): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findAll(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): IO[DaoResponse[PaginatedResult[Scope]]] = {
    for {
      xa ← db.transactor
      result ← findAllInternal(maybeLimit, maybeOffset).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findAll: " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def update(
    id: UUID,
    description: Option[String],
    updatedBy: String): IO[DaoResponse[Option[Scope]]] = {
    for {
      xa ← db.transactor
      maybeUpdatedScopeOrError ← updateInternal(id, description, updatedBy).transact(xa).attempt
    } yield maybeUpdatedScopeOrError.leftMap { exc ⇒
      val msg = s"Unexpected error in .update($id,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def remove(id: UUID, updatedBy: String): IO[DaoResponse[Option[UUID]]] = {
    (for {
      xa ← db.transactor
      maybeId ← removeInternal(id, updatedBy).transact(xa)
    } yield maybeId).attempt.map(_.leftMap { exc ⇒
      val msg = s"Unexpected error in .update($id,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    })
  }

  private[hikari] def createInternal(
    parentId: Option[UUID],
    name: String,
    description: Option[String],
    createdBy: String): ConnectionIO[Scope] = {
    val id = UUID.randomUUID()
    for {
      _ ← insertQuery(id, parentId, name, description, createdBy).update.run
      maybeScope ← fetchByIdInternal(id)
    } yield maybeScope
  }

  private[hikari] def reactivateInternal(
    parentId: Option[UUID],
    name: String,
    description: Option[String],
    createdBy: String): ConnectionIO[Scope] = {
    for {
      maybeExistingScope ← findByNameInternal(name)
      upsertedScope ← maybeExistingScope.fold {
        createInternal(parentId, name, description, createdBy)
      } { _ ⇒
        for {
          _ ← reactivateQuery(parentId, name, description, createdBy).update.run
          reactivatedScope ← fetchByNameInternal(name)
        } yield reactivatedScope
      }
    } yield upsertedScope
  }

  private[hikari] def findByIdInternal(id: UUID): ConnectionIO[Option[Scope]] = {
    queryById(id)
      .query[Scope]
      .option
  }

  private[hikari] def fetchByIdInternal(id: UUID): ConnectionIO[Scope] = {
    queryById(id)
      .query[Scope]
      .unique
  }

  private[hikari] def findByNameInternal(name: String): ConnectionIO[Option[Scope]] = {
    queryByName(name)
      .query[Scope]
      .option
  }

  private[hikari] def fetchByNameInternal(name: String): ConnectionIO[Scope] = {
    queryByName(name)
      .query[Scope]
      .unique
  }

  private[hikari] def findAllInternal(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): ConnectionIO[PaginatedResult[Scope]] = {
    for {
      page ← queryAll(maybeLimit, maybeOffset)
        .query[Scope]
        .to[List]
      total ← countAll.query[Int].unique
    } yield PaginatedResult(total, page, maybeLimit.getOrElse(Int.MaxValue), maybeOffset.getOrElse(0))
  }

  private[hikari] def updateInternal(
    id: UUID,
    description: Option[String],
    updatedBy: String): ConnectionIO[Option[Scope]] = {
    for {
      _ ← updateQuery(id, description, updatedBy).update.run
      maybeScope ← findByIdInternal(id)
    } yield maybeScope
  }

  private[hikari] def removeInternal(
    id: UUID,
    updatedBy: String): ConnectionIO[Option[UUID]] = {
    val uTime = nowUTC
    (for {
      existingScope ← OptionT(findByIdInternal(id))
      _ ← OptionT.liftF(PermissionHikariDao.deleteByScopeId(id, updatedBy, uTime).update.run)
      _ ← OptionT.liftF(deleteQuery(id, updatedBy, uTime).update.run)
    } yield existingScope.id).value
  }
}

object ScopeHikariDao {
  import HikariDBFApi._

  val TableName: Fragment = Fragment.const("scopes")

  private val SelectFromTable: Fragment =
    Fragment.const("SELECT `id`, `parentId`, `name`, `description`, `cBy`, `uBy`, `cDate`, `uDate` FROM") ++ TableName

  private val SelectCountFromTable: Fragment =
    Fragment.const("SELECT COUNT(`id`) FROM") ++ TableName

  private val InsertIntoTable: Fragment = fr"INSERT INTO " ++ TableName ++
    fr"(`id`, `parentId`, `name`, `description`, `cBy`, `uBy`) VALUES"

  private val UpdateTable: Fragment = fr"UPDATE" ++ TableName

  private val whereActive: Fragment = fr"WHERE status = 1"

  private def whereByIdAndActive(id: UUID): Fragment = fr"WHERE id = $id and status = 1;"

  private def whereByNameAndActive(name: String): Fragment = fr"WHERE name = $name and status = 1;"

  private def queryById(id: UUID): Fragment = SelectFromTable ++ whereByIdAndActive(id)

  private def queryByName(name: String): Fragment = SelectFromTable ++ whereByNameAndActive(name)

  private def queryAll(maybeLimit: Option[Int], maybeOffset: Option[Int]): Fragment = {
    val limit = maybeLimit.fold(EmptyFragment)(lmt ⇒ fr"LIMIT $lmt")
    val offset = maybeOffset.fold(EmptyFragment)(off ⇒ fr"OFFSET $off")
    SelectFromTable ++ fr"WHERE status = 1" ++ limit ++ offset
  }

  private def countAll: Fragment = SelectCountFromTable ++ whereActive

  private def insertQuery(
    id: UUID,
    parentId: Option[UUID],
    name: String,
    description: Option[String],
    createdBy: String): Fragment = {
    InsertIntoTable ++ fr"($id, $parentId, $name, $description, $createdBy, $createdBy);"
  }

  private def reactivateQuery(
    parentId: Option[UUID],
    name: String,
    description: Option[String],
    createdBy: String): Fragment = {
    UpdateTable ++ fr"SET `parentId` = $parentId, `description` = $description, uBy = $createdBy" ++
      whereByNameAndActive(name)
  }

  private def updateQuery(
    id: UUID,
    description: Option[String],
    updatedBy: String): Fragment = {
    UpdateTable ++ fr"SET `description` = $description, uBy = $updatedBy" ++ whereByIdAndActive(id)
  }

  private def deleteQuery(
    id: UUID,
    updatedBy: String,
    updatedAt: ZonedDateTime): Fragment = {
    UpdateTable ++ fr"SET status = 0, uBy = $updatedBy, uDate = $updatedAt" ++ whereByIdAndActive(id)
  }
}
