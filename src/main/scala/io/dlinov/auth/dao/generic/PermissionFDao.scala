package io.dlinov.auth.dao.generic

import java.util.UUID

import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Permission
import io.dlinov.auth.routes.dto.PermissionKey
import io.dlinov.auth.dao.Dao.{DaoResponse, EntityId}
import io.dlinov.auth.domain.auth.entities.Permission
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.PermissionKey

trait PermissionFDao[F[_]] extends Dao {

  def create(
      pKey: PermissionKey,
      scopeId: EntityId,
      revoke: Boolean,
      createdBy: String,
      reactivate: Boolean
  ): F[DaoResponse[Permission]]

  def findById(id: UUID): F[DaoResponse[Option[Permission]]]

  def findAndMerge(
      businessUnitId: UUID,
      roleId: UUID,
      maybeUserId: Option[UUID],
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[DaoResponse[PaginatedResult[Permission]]]

  def update(
      id: EntityId,
      mbPermissionKey: Option[PermissionKey],
      mbScopeId: Option[UUID],
      updatedBy: String
  ): F[DaoResponse[Option[Permission]]]

  def remove(id: EntityId, updatedBy: String): F[DaoResponse[Option[Permission]]]
}
