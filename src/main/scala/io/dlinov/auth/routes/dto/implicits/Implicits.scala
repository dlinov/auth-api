package io.dlinov.auth.routes.dto.implicits

import java.util.UUID

import io.circe.parser._
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, BusinessUnit, Permission, Role, Scope}
import io.dlinov.auth.routes.dto.{
  BackOfficeUserToRead,
  BusinessUnitToRead,
  CollectionResponse,
  Document,
  DocumentToRead,
  PermissionBlueprint,
  PermissionToCreate,
  PermissionToRead,
  RoleToRead,
  ScopeToRead,
  WrappedId
}
import io.dlinov.auth.domain.auth.entities._
import io.dlinov.auth.routes.dto._

object Implicits {

  implicit class ScopeConverter(val s: Scope) extends AnyVal {
    def asApi =
      ScopeToRead(
        id = s.id,
        parentId = s.parentId,
        name = s.name,
        description = s.description,
        createdBy = s.createdBy,
        updatedBy = s.updatedBy,
        createdTime = s.createdTime,
        updatedTime = s.updatedTime
      )
  }

  implicit class PermissionConverter(val p: Permission) extends AnyVal {
    def asApi =
      PermissionToRead(
        id = p.id,
        scope = p.scope.asApi,
        createdBy = p.createdBy,
        updatedBy = p.updatedBy,
        createdTime = p.createdTime,
        updatedTime = p.updatedTime
      )
  }

  implicit class PermissionToCreateConverter(val p: PermissionToCreate) extends AnyVal {
    def asDomain(createdBy: String): PermissionBlueprint = {
      PermissionBlueprint(
        revoke = p.revoke.getOrElse(false),
        pKey = p.permissionKey,
        scopeId = p.scopeId,
        createdBy = createdBy
      )
    }
  }

  implicit class RoleConverter(val role: Role) extends AnyVal {
    def asApi =
      RoleToRead(
        id = role.id,
        name = role.name,
        createdBy = role.createdBy,
        updatedBy = role.updatedBy,
        createdTime = role.createdTime,
        updatedTime = role.updatedTime
      )
  }

  implicit class BusinessUnitConverter(val bu: BusinessUnit) extends AnyVal {
    def asApi =
      BusinessUnitToRead(
        id = bu.id,
        name = bu.name,
        createdBy = bu.createdBy,
        updatedBy = bu.updatedBy,
        createdTime = bu.createdTime,
        updatedTime = bu.updatedTime
      )
  }

  implicit class UserConverter(val u: BackOfficeUser) extends AnyVal {
    def asApi =
      BackOfficeUserToRead(
        id = u.id,
        userName = u.userName,
        email = u.email.value,
        phoneNumber = u.phoneNumber,
        firstName = u.firstName,
        middleName = u.middleName,
        lastName = u.lastName,
        description = u.description,
        homePage = u.homePage,
        activeLanguage = u.activeLanguage,
        lastLoginTimestamp = u.lastLoginTimestamp,
        customData = u.customData.flatMap(parse(_).toOption),
        role = u.role.asApi,
        businessUnit = u.businessUnit.asApi,
        permissions = u.permissions.map(_.asApi),
        createdBy = u.createdBy,
        updatedBy = u.updatedBy,
        createdTime = u.createdTime,
        updatedTime = u.updatedTime
      )
  }

  implicit class DocumentConverter(val d: Document) extends AnyVal {
    def asApi =
      DocumentToRead(
        id = d.documentId,
        customerId = d.customerId,
        documentType = d.documentType,
        documentTypeIdentifier = d.documentTypeIdentifier,
        purpose = d.purpose,
        link = d.link,
        createdBy = d.createdBy,
        createdAt = d.createdAt
      )
  }

  implicit class CollectionConverter[T](val seq: Seq[T]) extends AnyVal {
    def col = CollectionResponse(seq, seq.size)
  }

  implicit class PaginatedResultFunctor[T](val page: PaginatedResult[T]) extends AnyVal {
    def map[R](f: T ⇒ R): PaginatedResult[R] = {
      val convertedResults = page.results.map(f)
      page.copy(results = convertedResults)
    }
  }

  implicit class UUIDOps(val id: UUID) {
    def asApi = WrappedId(id)
  }
}
