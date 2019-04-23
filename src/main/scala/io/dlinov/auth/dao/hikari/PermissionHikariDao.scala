package io.dlinov.auth.dao.hikari

import java.time.ZonedDateTime
import java.util.UUID

import cats.effect.IO
import cats.syntax.either._
import doobie._
import doobie.implicits._
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.PermissionFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Permission
import io.dlinov.auth.routes.dto.{PermissionKey, PermissionKeys}
import io.dlinov.auth.dao.Dao.{DaoResponse, EntityId}
import io.dlinov.auth.domain.auth.entities.Permission
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.PermissionFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}
import io.dlinov.auth.routes.dto.{PermissionKey, PermissionKeys}

import scala.collection.immutable.HashMap

class PermissionHikariDao(db: DBFApi[IO])
  extends PermissionFDao {

  import HikariDBFApi._
  import PermissionHikariDao._

  override def create(
    pKey: PermissionKey,
    scopeId: EntityId,
    revoke: Boolean,
    createdBy: String,
    reactivate: Boolean): IO[DaoResponse[Permission]] = {
    for {
      xa ← db.transactor
      result ← (if (reactivate) {
        reactivateInternal(pKey, scopeId, revoke, createdBy)
      } else {
        createInternal(pKey, scopeId, revoke, createdBy)
      }).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .create($pKey,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findById(id: UUID): IO[DaoResponse[Option[Permission]]] = {
    for {
      xa ← db.transactor
      result ← findByIdInternal(id).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findById($id): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findAndMerge(
    businessUnitId: UUID,
    roleId: UUID,
    maybeUserId: Option[UUID],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): IO[DaoResponse[PaginatedResult[Permission]]] = {
    for {
      xa ← db.transactor
      result ← {
        findAndMergeInternal(businessUnitId, roleId, maybeUserId, maybeLimit, maybeOffset)
          .transact(xa).attempt
      }
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findAndMerge(b=$businessUnitId, r=$roleId, u=$maybeUserId): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def update(
    id: EntityId,
    mbPermissionKey: Option[PermissionKey],
    mbScopeId: Option[UUID],
    updatedBy: String): IO[DaoResponse[Option[Permission]]] = {
    for {
      xa ← db.transactor
      result ← updateInternal(id, mbPermissionKey, mbScopeId, updatedBy).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .update($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def remove(id: EntityId, updatedBy: String): IO[DaoResponse[Option[Permission]]] = {
    for {
      xa ← db.transactor
      result ← removeInternal(id, updatedBy).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .remove($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  private[hikari] def findByIdInternal(id: UUID): ConnectionIO[Option[Permission]] = {
    findOneInternal(queryById(id))
  }

  private[hikari] def findByPermissionKeyInternal(
    pKey: PermissionKey,
    scopeId: UUID,
    onlyActive: Boolean): ConnectionIO[Option[Permission]] = {
    findOneInternal(queryByPermissionKey(pKey, scopeId, onlyActive))
  }

  private[hikari] def fetchByIdInternal(id: UUID): ConnectionIO[Permission] = {
    fetchOneInternal(queryById(id))
  }

  private[hikari] def findAndMergeInternal(
    businessUnitId: UUID,
    roleId: UUID,
    maybeUserId: Option[UUID],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): ConnectionIO[PaginatedResult[Permission]] = {
    val burPermissions = findManyInternal(queryByBusinessUnitIdAndRoleId(businessUnitId, roleId))
    val allPermissions = maybeUserId.fold {
      burPermissions
    } { userId ⇒
      for {
        burPs ← burPermissions
        uPs ← findManyInternal(queryByUserId(userId))
      } yield {
        val burMap = makeMergeableMapOfPermissions(burPs)
        val uMap = makeMergeableMapOfPermissions(uPs)
        burMap
          .merged(uMap)(mergePermissions)
          .collect {
            // negativePermissions are eliminated here
            case (_, v) if v.status > 0 ⇒ v
          }
          .toList
      }
    }
    allPermissions.map { ps ⇒
      val permissionsPage = (maybeLimit, maybeOffset) match {
        case (Some(limit), Some(offset)) ⇒
          ps.slice(offset, offset + limit)
        case (Some(limit), _) ⇒
          ps.take(limit)
        case (_, Some(offset)) ⇒
          ps.drop(offset)
        case _ ⇒
          ps
      }
      PaginatedResult(
        total = ps.size,
        results = permissionsPage,
        limit = maybeLimit.getOrElse(Int.MaxValue),
        offset = maybeOffset.getOrElse(0))
    }
  }

  private[hikari] def createInternal(
    pKey: PermissionKey,
    scopeId: EntityId,
    revoke: Boolean,
    createdBy: String): ConnectionIO[Permission] = {
    val id = UUID.randomUUID()
    for {
      _ ← insertQuery(id, pKey, scopeId, revoke, createdBy).update.run
      created ← fetchByIdInternal(id)
    } yield created
  }

  private[hikari] def reactivateInternal(
    pKey: PermissionKey,
    scopeId: EntityId,
    revoke: Boolean,
    createdBy: String): ConnectionIO[Permission] = {
    for {
      maybeExistingScope ← findByPermissionKeyInternal(pKey, scopeId, onlyActive = false)
      reactivated ← maybeExistingScope.fold {
        createInternal(pKey, scopeId, revoke, createdBy)
      } { existing ⇒
        for {
          _ ← reactivateQuery(existing.id, revoke, createdBy, nowUTC).update.run
          p ← fetchByIdInternal(existing.id)
        } yield p
      }
    } yield reactivated
  }

  private[hikari] def updateInternal(
    id: UUID,
    mbPermissionKey: Option[PermissionKey],
    mbScopeId: Option[UUID],
    updatedBy: String): ConnectionIO[Option[Permission]] = {
    for {
      _ ← updateQuery(id, mbPermissionKey, mbScopeId, updatedBy, nowUTC).update.run
      maybeExisting ← findByIdInternal(id)
    } yield maybeExisting
  }

  private[hikari] def removeInternal(
    id: UUID,
    updatedBy: String): ConnectionIO[Option[Permission]] = {
    for {
      maybeExisting ← findByIdInternal(id)
      _ ← removeQuery(id, updatedBy).update.run
    } yield maybeExisting
  }

  protected def mergePermissions[T]: (T, T) ⇒ T = (_, right) ⇒ right

  private def makeMergeableMapOfPermissions(ps: Seq[Permission]) = HashMap(ps.map(_.scope.name).zip(ps): _*)

  private def findOneInternal(fr: Fragment): ConnectionIO[Option[Permission]] = {
    fr.query[Permission].option
  }

  private def fetchOneInternal(fr: Fragment): ConnectionIO[Permission] = {
    fr.query[Permission].unique
  }

  private def findManyInternal(fr: Fragment): ConnectionIO[List[Permission]] = {
    fr.query[Permission].to[List]
  }
}

object PermissionHikariDao {
  import HikariDBFApi._

  private val EmptyFragment = Fragment.const("")
  val TableName: Fragment = Fragment.const("permissions")

  private val SelectFromTable: Fragment =
    Fragment.const("SELECT p.id, p.buId, p.userId, p.roleId," +
      "p.scopeId, s.parentId, s.name, s.description, s.cBy, s.uBy, s.cDate, s.uDate," +
      "p.cBy, p.uBy, p.status, p.cDate, p.uDate FROM ") ++ TableName ++ fr"p" ++
      fr"INNER JOIN" ++ ScopeHikariDao.TableName ++ fr"s ON p.scopeId = s.id"

  private val InsertIntoTable: Fragment = Fragment.const("INSERT INTO ") ++ TableName ++
    Fragment.const(" (id, buId, userId, roleId, scopeId, canWrite, status, cBy, uBy) VALUES ")

  private val UpdateTable: Fragment = Fragment.const("UPDATE ") ++ TableName

  private def queryById(id: UUID): Fragment = SelectFromTable ++ fr"WHERE p.id = $id AND p.status <> 0;"

  private def queryByPermissionKey(pKey: PermissionKey, scopeId: UUID, onlyActive: Boolean): Fragment = {
    val statusPredicate = if (onlyActive) {
      fr"AND p.status <> 0;"
    } else {
      fr";"
    }
    val predicate = pKey match {
      case BusinessUnitAndRolePermissionKey(buId, roleId) ⇒
        fr"WHERE p.buId = $buId AND p.roleId = $roleId AND p.scopeId = $scopeId"
      case UserPermissionKey(userId) ⇒
        fr"WHERE p.userId = $userId AND p.scopeId = $scopeId"
    }
    SelectFromTable ++ predicate ++ statusPredicate
  }

  private def queryByBusinessUnitIdAndRoleId(
    businessUnitId: UUID,
    roleId: UUID): Fragment = {
    SelectFromTable ++ fr"WHERE p.buId = $businessUnitId AND p.roleId = $roleId AND p.status <> 0;"
  }

  private def queryByUserId(userId: UUID): Fragment = {
    SelectFromTable ++ fr"WHERE p.userId = $userId AND p.status <> 0;"
  }

  private def insertQuery(
    id: UUID,
    permissionKey: PermissionKey,
    scopeId: UUID,
    revoke: Boolean,
    createdBy: String): Fragment = {
    val status = if (revoke) -1 else 1
    val vals = permissionKey match {
      case PermissionKeys.BusinessUnitAndRolePermissionKey(buId, roleId) ⇒
        fr"($id, $buId, NULL, $roleId, $scopeId, 1, $status, $createdBy, $createdBy)"
      case PermissionKeys.UserPermissionKey(userId) ⇒
        fr"($id, NULL, $userId, NULL, $scopeId, 1, $status, $createdBy, $createdBy)"
    }
    InsertIntoTable ++ vals
  }

  def reactivateQuery(
    id: UUID,
    revoke: Boolean,
    uBy: String,
    uAt: ZonedDateTime): Fragment = {
    val status = if (revoke) -1 else 1
    UpdateTable ++ fr"SET status = $status, uBy = $uBy, uDate = $uAt WHERE id = $id"
  }

  private[hikari] def updateQuery(
    id: UUID,
    mbPermissionKey: Option[PermissionKey],
    mbScopeId: Option[UUID],
    uBy: String,
    uAt: ZonedDateTime): Fragment = {
    val updPermKey = mbPermissionKey.fold(EmptyFragment) {
      case BusinessUnitAndRolePermissionKey(buId, roleId) ⇒
        fr"buId = $buId, userId = NULL, roleId = $roleId,"
      case UserPermissionKey(userId) ⇒
        fr"buId = NULL, userId = $userId, roleId = NULL,"
    }
    val updScope = mbScopeId.fold(EmptyFragment) { scopeId ⇒
      fr"scopeId = $scopeId,"
    }
    UpdateTable ++ fr"SET" ++ updPermKey ++ updScope ++ fr"uBy = $uBy, uDate = $uAt WHERE id = $id"
  }

  private[hikari] def removeQuery(id: UUID, uBy: String): Fragment = {
    UpdateTable ++ fr"SET status = 0, uBy = $uBy WHERE id = $id"
  }

  private[hikari] def deleteByScopeId(scopeId: UUID, uBy: String, uAt: ZonedDateTime): Fragment = {
    UpdateTable ++ fr"SET status = 0, uBy = $uBy, uDate = $uAt WHERE scopeId = $scopeId"
  }
}
