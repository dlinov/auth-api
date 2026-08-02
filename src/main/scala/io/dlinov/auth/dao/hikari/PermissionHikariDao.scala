package io.dlinov.auth.dao.hikari

import java.time.ZonedDateTime
import java.util.UUID

import cats.effect.Async
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import doobie._
import doobie.implicits._
import io.dlinov.auth.dao.Dao.{DaoResponse, EntityId}
import io.dlinov.auth.domain.auth.entities.Permission
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.PermissionFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.PermissionKeys.{
  BusinessUnitAndRolePermissionKey,
  UserPermissionKey
}
import io.dlinov.auth.routes.dto.{PermissionKey, PermissionKeys}

import scala.collection.immutable.HashMap

class PermissionHikariDao[F[_]: Async](db: DBFApi[F]) extends PermissionFDao[F] {

  import HikariDBFApi._
  import PermissionHikariDao._

  override def create(
      pKey: PermissionKey,
      scopeId: EntityId,
      revoke: Boolean,
      createdBy: String,
      reactivate: Boolean
  ): F[DaoResponse[Permission]] = {
    for {
      xa     <- db.transactor
      result <- (if (reactivate) {
                   reactivateInternal(pKey, scopeId, revoke, createdBy)
                 } else {
                   createInternal(pKey, scopeId, revoke, createdBy)
                 }).transact(xa).attemptSql
    } yield result.leftMap { exc =>
      val msg = s"Unexpected error in .create($pKey,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findById(id: UUID): F[DaoResponse[Option[Permission]]] = {
    for {
      xa     <- db.transactor
      result <- findByIdInternal(id).transact(xa).attemptSql
    } yield result.leftMap { exc =>
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
      maybeOffset: Option[Int]
  ): F[DaoResponse[PaginatedResult[Permission]]] = {
    for {
      xa     <- db.transactor
      result <- {
        findAndMergeInternal(businessUnitId, roleId, maybeUserId, maybeLimit, maybeOffset)
          .transact(xa)
          .attemptSql
      }
    } yield result.leftMap { exc =>
      val msg =
        s"Unexpected error in .findAndMerge(b=$businessUnitId, r=$roleId, u=$maybeUserId): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def update(
      id: EntityId,
      mbPermissionKey: Option[PermissionKey],
      mbScopeId: Option[UUID],
      updatedBy: String
  ): F[DaoResponse[Option[Permission]]] = {
    for {
      xa     <- db.transactor
      result <- updateInternal(id, mbPermissionKey, mbScopeId, updatedBy).transact(xa).attemptSql
    } yield result.leftMap { exc =>
      val msg = s"Unexpected error in .update($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def remove(id: EntityId, updatedBy: String): F[DaoResponse[Option[Permission]]] = {
    for {
      xa     <- db.transactor
      result <- removeInternal(id, updatedBy).transact(xa).attemptSql
    } yield result.leftMap { exc =>
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
      onlyActive: Boolean
  ): ConnectionIO[Option[Permission]] = {
    findOneInternal(queryByPermissionKey(pKey, scopeId, onlyActive))
  }

  private[hikari] def fetchByIdInternal(id: UUID): ConnectionIO[Permission] = {
    fetchOneInternal(queryById(id))
  }

  /** Effective permissions for a single (businessUnitId, roleId[, userId]) combination.
    *
    * Rather than fetching every BU/role permission plus every user-specific permission unbounded,
    * merging them in Scala and then slicing the merged list in memory, this pushes the merge (user
    * permission overrides its BU/role counterpart per scope, negative/revoked effective permissions
    * are dropped) and the pagination into a single SQL query per branch, so only the requested page
    * is ever materialized on the JVM side.
    */
  private[hikari] def findAndMergeInternal(
      businessUnitId: UUID,
      roleId: UUID,
      maybeUserId: Option[UUID],
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): ConnectionIO[PaginatedResult[Permission]] = {
    val (page, total) = maybeUserId match {
      case None =>
        (
          findManyInternal(
            queryByBusinessUnitIdAndRoleId(businessUnitId, roleId, maybeLimit, maybeOffset)
          ),
          countByBusinessUnitIdAndRoleId(businessUnitId, roleId).query[Int].unique
        )
      case Some(userId) =>
        (
          findManyInternal(
            queryMergedEffective(businessUnitId, roleId, userId, maybeLimit, maybeOffset)
          ),
          countMergedEffective(businessUnitId, roleId, userId).query[Int].unique
        )
    }
    for {
      results <- page
      count   <- total
    } yield PaginatedResult(
      total = count,
      results = results,
      limit = maybeLimit.getOrElse(Int.MaxValue),
      offset = maybeOffset.getOrElse(0)
    )
  }

  /** Batched, N+1-free variant of the merge above for a whole page of users at once: instead of one
    * `findAndMergeInternal` round-trip per user (2 queries each), fetches all candidate BU/role
    * permissions for the distinct (businessUnitId, roleId) pairs present on the page in a single
    * query, and all user-specific overrides for every user id on the page in another single query,
    * then merges per user in memory using the same semantics as `findAndMergeInternal`'s
    * `Some(userId)` branch (user overrides win per scope; the merged result keeps only `status > 0`
    * rows).
    */
  private[hikari] def findManyAndMergeInternal(
      users: List[(UUID, UUID, UUID)] // (userId, businessUnitId, roleId)
  ): ConnectionIO[Map[UUID, List[Permission]]] = {
    val buRolePairs = users.map { case (_, buId, roleId) => (buId, roleId) }.toSet
    val userIds     = users.map(_._1).toSet
    for {
      burPs <- findManyInternal(queryByBusinessUnitRolePairs(buRolePairs))
      uPs   <- findManyInternal(queryByUserIds(userIds))
    } yield {
      val burByPair = burPs.groupBy(pairOf)
      val uByUser   = uPs.groupBy(userIdOf)
      users.map { case (userId, buId, roleId) =>
        val baseline  = burByPair.getOrElse((buId, roleId), Nil)
        val overrides = uByUser.getOrElse(userId, Nil)
        userId -> mergeEffectivePermissions(baseline, overrides)
      }.toMap
    }
  }

  private def pairOf(p: Permission): (UUID, UUID) = p.permissionKey match {
    case BusinessUnitAndRolePermissionKey(buId, roleId) => (buId, roleId)
    case UserPermissionKey(userId)                      =>
      throw new IllegalStateException(s"Expected a BU/role permission, got user one: $userId")
  }

  private def userIdOf(p: Permission): UUID = p.permissionKey match {
    case UserPermissionKey(userId)                      => userId
    case BusinessUnitAndRolePermissionKey(buId, roleId) =>
      throw new IllegalStateException(
        s"Expected a user permission, got bu/role one: ($buId,$roleId)"
      )
  }

  private[hikari] def mergeEffectivePermissions(
      baseline: Seq[Permission],
      overrides: Seq[Permission]
  ): List[Permission] = {
    val baselineMap = makeMergeableMapOfPermissions(baseline)
    val overrideMap = makeMergeableMapOfPermissions(overrides)
    baselineMap
      .merged(overrideMap)(mergePermissions)
      .collect {
        // negative/revoked effective permissions are eliminated here
        case (_, v) if v.status > 0 => v
      }
      .toList
  }

  private[hikari] def createInternal(
      pKey: PermissionKey,
      scopeId: EntityId,
      revoke: Boolean,
      createdBy: String
  ): ConnectionIO[Permission] = {
    val id = UUID.randomUUID()
    for {
      _       <- insertQuery(id, pKey, scopeId, revoke, createdBy).update.run
      created <- fetchByIdInternal(id)
    } yield created
  }

  private[hikari] def reactivateInternal(
      pKey: PermissionKey,
      scopeId: EntityId,
      revoke: Boolean,
      createdBy: String
  ): ConnectionIO[Permission] = {
    for {
      maybeExistingScope <- findByPermissionKeyInternal(pKey, scopeId, onlyActive = false)
      reactivated        <- maybeExistingScope.fold {
        createInternal(pKey, scopeId, revoke, createdBy)
      } { existing =>
        for {
          _ <- reactivateQuery(existing.id, revoke, createdBy, nowUTC).update.run
          p <- fetchByIdInternal(existing.id)
        } yield p
      }
    } yield reactivated
  }

  private[hikari] def updateInternal(
      id: UUID,
      mbPermissionKey: Option[PermissionKey],
      mbScopeId: Option[UUID],
      updatedBy: String
  ): ConnectionIO[Option[Permission]] = {
    for {
      _             <- updateQuery(id, mbPermissionKey, mbScopeId, updatedBy, nowUTC).update.run
      maybeExisting <- findByIdInternal(id)
    } yield maybeExisting
  }

  private[hikari] def removeInternal(
      id: UUID,
      updatedBy: String
  ): ConnectionIO[Option[Permission]] = {
    for {
      maybeExisting <- findByIdInternal(id)
      _             <- removeQuery(id, updatedBy).update.run
    } yield maybeExisting
  }

  protected def mergePermissions[T]: (T, T) => T = (_, right) => right

  private def makeMergeableMapOfPermissions(ps: Seq[Permission]) =
    HashMap(ps.map(_.scope.name).zip(ps)*)

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
  val TableName: Fragment   = Fragment.const("permissions")

  private val SelectFromTable: Fragment =
    Fragment.const(
      "SELECT p.id, p.buId, p.userId, p.roleId," +
        "p.scopeId, s.parentId, s.name, s.description, s.cBy, s.uBy, s.cDate, s.uDate," +
        "p.cBy, p.uBy, p.status, p.cDate, p.uDate FROM "
    ) ++ TableName ++ fr"p" ++
      fr"INNER JOIN" ++ ScopeHikariDao.TableName ++ fr"s ON p.scopeId = s.id"

  private val InsertIntoTable: Fragment = Fragment.const("INSERT INTO ") ++ TableName ++
    Fragment.const(" (id, buId, userId, roleId, scopeId, canWrite, status, cBy, uBy) VALUES ")

  private val UpdateTable: Fragment = Fragment.const("UPDATE ") ++ TableName

  private def queryById(id: UUID): Fragment =
    SelectFromTable ++ fr"WHERE p.id = $id AND p.status <> 0;"

  private def queryByPermissionKey(
      pKey: PermissionKey,
      scopeId: UUID,
      onlyActive: Boolean
  ): Fragment = {
    val statusPredicate = if (onlyActive) {
      fr"AND p.status <> 0;"
    } else {
      fr";"
    }
    val predicate = pKey match {
      case BusinessUnitAndRolePermissionKey(buId, roleId) =>
        fr"WHERE p.buId = $buId AND p.roleId = $roleId AND p.scopeId = $scopeId"
      case UserPermissionKey(userId) =>
        fr"WHERE p.userId = $userId AND p.scopeId = $scopeId"
    }
    SelectFromTable ++ predicate ++ statusPredicate
  }

  private def queryByBusinessUnitIdAndRoleId(
      businessUnitId: UUID,
      roleId: UUID,
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): Fragment = {
    val limit  = maybeLimit.fold(EmptyFragment)(lmt => fr"LIMIT $lmt")
    val offset = maybeOffset.fold(EmptyFragment)(off => fr"OFFSET $off")
    SelectFromTable ++
      fr"WHERE p.buId = $businessUnitId AND p.roleId = $roleId AND p.status <> 0" ++
      fr"ORDER BY s.parentId, s.name" ++ limit ++ offset
  }

  private def countByBusinessUnitIdAndRoleId(businessUnitId: UUID, roleId: UUID): Fragment = {
    Fragment.const("SELECT COUNT(p.id) FROM ") ++ TableName ++ fr"p" ++
      fr"WHERE p.buId = $businessUnitId AND p.roleId = $roleId AND p.status <> 0"
  }

  /** Shared `WITH` clause computing, per scope, the single effective permission row for `userId`:
    * the user-specific permission if one exists for that scope, otherwise the business-unit/role
    * permission. Built as a single query (rather than two round-trips merged/paginated in Scala)
    * via a `UNION ALL` of both candidate sources tagged with a priority, keeping only the
    * highest-priority row per scope via `ROW_NUMBER()`.
    */
  private def mergedEffectiveCte(businessUnitId: UUID, roleId: UUID, userId: UUID): Fragment = {
    Fragment.const(
      """WITH candidate AS (
        |  SELECT id, buId, userId, roleId, scopeId, status, cBy, uBy, cDate, uDate, 1 AS priority
        |    FROM permissions
        |   WHERE """.stripMargin
    ) ++ fr"buId = $businessUnitId AND roleId = $roleId AND status <> 0" ++
      Fragment.const(
        """
          |  UNION ALL
          |  SELECT id, buId, userId, roleId, scopeId, status, cBy, uBy, cDate, uDate, 2 AS priority
          |    FROM permissions
          |   WHERE """.stripMargin
      ) ++ fr"userId = $userId AND status <> 0" ++
      Fragment.const(
        """
          |), ranked AS (
          |  SELECT c.*, ROW_NUMBER() OVER (PARTITION BY c.scopeId ORDER BY c.priority DESC) AS rn
          |    FROM candidate c
          |)
          |""".stripMargin
      )
  }

  private def queryMergedEffective(
      businessUnitId: UUID,
      roleId: UUID,
      userId: UUID,
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): Fragment = {
    val limit  = maybeLimit.fold(EmptyFragment)(lmt => fr"LIMIT $lmt")
    val offset = maybeOffset.fold(EmptyFragment)(off => fr"OFFSET $off")
    mergedEffectiveCte(businessUnitId, roleId, userId) ++
      Fragment.const(
        """SELECT r.id, r.buId, r.userId, r.roleId,
          |       r.scopeId, s.parentId, s.name, s.description, s.cBy, s.uBy, s.cDate, s.uDate,
          |       r.cBy, r.uBy, r.status, r.cDate, r.uDate
          |  FROM ranked r
          |  INNER JOIN """.stripMargin
      ) ++ ScopeHikariDao.TableName ++ Fragment.const(" s ON r.scopeId = s.id") ++
      fr"WHERE r.rn = 1 AND r.status > 0 ORDER BY s.parentId, s.name" ++ limit ++ offset
  }

  private def countMergedEffective(businessUnitId: UUID, roleId: UUID, userId: UUID): Fragment = {
    mergedEffectiveCte(businessUnitId, roleId, userId) ++
      Fragment.const("SELECT COUNT(*) FROM ranked WHERE rn = 1 AND status > 0")
  }

  private def queryByBusinessUnitRolePairs(pairs: Set[(UUID, UUID)]): Fragment = {
    if (pairs.isEmpty) {
      SelectFromTable ++ fr"WHERE 1 = 0"
    } else {
      val predicate = pairs.toList
        .map { case (buId, roleId) => fr"(p.buId = $buId AND p.roleId = $roleId)" }
        .reduceLeft(_ ++ fr"OR" ++ _)
      SelectFromTable ++ fr"WHERE (" ++ predicate ++ fr") AND p.status <> 0"
    }
  }

  private def queryByUserIds(userIds: Set[UUID]): Fragment = {
    if (userIds.isEmpty) {
      SelectFromTable ++ fr"WHERE 1 = 0"
    } else {
      val idList = userIds.toList.map(id => fr"$id").reduceLeft(_ ++ fr"," ++ _)
      SelectFromTable ++ Fragment.const("WHERE p.userId IN (") ++ idList ++
        Fragment.const(")") ++ fr"AND p.status <> 0"
    }
  }

  private def insertQuery(
      id: UUID,
      permissionKey: PermissionKey,
      scopeId: UUID,
      revoke: Boolean,
      createdBy: String
  ): Fragment = {
    val status = if (revoke) -1 else 1
    val vals   = permissionKey match {
      case PermissionKeys.BusinessUnitAndRolePermissionKey(buId, roleId) =>
        fr"($id, $buId, NULL, $roleId, $scopeId, 1, $status, $createdBy, $createdBy)"
      case PermissionKeys.UserPermissionKey(userId) =>
        fr"($id, NULL, $userId, NULL, $scopeId, 1, $status, $createdBy, $createdBy)"
    }
    InsertIntoTable ++ vals
  }

  def reactivateQuery(id: UUID, revoke: Boolean, uBy: String, uAt: ZonedDateTime): Fragment = {
    val status = if (revoke) -1 else 1
    UpdateTable ++ fr"SET status = $status, uBy = $uBy, uDate = $uAt WHERE id = $id"
  }

  private[hikari] def updateQuery(
      id: UUID,
      mbPermissionKey: Option[PermissionKey],
      mbScopeId: Option[UUID],
      uBy: String,
      uAt: ZonedDateTime
  ): Fragment = {
    val updPermKey = mbPermissionKey.fold(EmptyFragment) {
      case BusinessUnitAndRolePermissionKey(buId, roleId) =>
        fr"buId = $buId, userId = NULL, roleId = $roleId,"
      case UserPermissionKey(userId) =>
        fr"buId = NULL, userId = $userId, roleId = NULL,"
    }
    val updScope = mbScopeId.fold(EmptyFragment) { scopeId =>
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
