package io.dlinov.auth.domain.algebras

import java.util.UUID

import cats.Functor
import cats.syntax.functor._
import io.dlinov.auth.dao.generic.PermissionFDao
import io.dlinov.auth.domain.auth.entities.Permission
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.routes.dto.{PermissionBlueprint, PermissionKey}
import io.dlinov.auth.domain.{BaseService, PaginatedResult, ServiceError}
import io.dlinov.auth.domain.auth.entities.Permission
import io.dlinov.auth.dao.generic.PermissionFDao
import io.dlinov.auth.routes.dto.{PermissionBlueprint, PermissionKey}

class PermissionAlgebra[F[_]: Functor](dao: PermissionFDao[F]) extends BaseService {
  import BaseService._

  def create(
      p: PermissionBlueprint,
      maybeReactivate: Option[Boolean]
  ): F[ServiceResponse[Permission]] = {
    val reactivate = maybeReactivate.getOrElse(false)
    for {
      pOrError ← dao.create(p.pKey, p.scopeId, p.revoke, p.createdBy, reactivate)
    } yield pOrError.asServiceResponse
  }

  def findBy(
      buId: UUID,
      rId: UUID,
      uId: Option[UUID],
      maybeLimit: Option[Int],
      maybeOffset: Option[Int]
  ): F[ServiceResponse[PaginatedResult[Permission]]] = {
    for {
      permissions ← dao.findAndMerge(buId, rId, uId, maybeLimit, maybeOffset)
    } yield permissions.asServiceResponse
  }

  def update(
      id: UUID,
      mbPermissionKey: Option[PermissionKey],
      mbScopeId: Option[UUID],
      updatedBy: String
  ): F[ServiceResponse[Permission]] = {
    for {
      maybePermissionOrError ← dao.update(id, mbPermissionKey, mbScopeId, updatedBy)
    } yield maybePermissionOrError.asServiceResponse
      .flatMap(
        _.toRight(ServiceError.notFoundEntityError(s"Permission id='$id' couldn't be found"))
      )
  }

  def remove(id: UUID, updatedBy: String): F[ServiceResponse[UUID]] = {
    for {
      maybePermissionOrError ← dao.remove(id, updatedBy)
    } yield maybePermissionOrError.asServiceResponse
      .flatMap(
        _.map(_.id)
          .toRight(ServiceError.notFoundEntityError(s"Permission id='$id' couldn't be found"))
      )
  }
}
