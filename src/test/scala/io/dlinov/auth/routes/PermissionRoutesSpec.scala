package io.dlinov.auth.routes

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.either._
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.dto.{PermissionKeys, PermissionToCreate, PermissionToRead, PermissionToUpdate}
import org.http4s.Status
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.dto.PermissionKeys.UserPermissionKey
import io.dlinov.auth.routes.dto.{PermissionKeys, PermissionToCreate, PermissionToRead, PermissionToUpdate}
import io.dlinov.auth.routes.json.EntityEncoders.{permissionToCreateEntityEncoder, permissionToUpdateEntityEncoder}
import io.dlinov.auth.routes.json.EntityDecoders.{pToReadEntityDecoder, pToReadPageEntityDecoder}

class PermissionRoutesSpec extends Http4sSpec {

  private val baseRoute = "/api/permissions"
  private val scopeName = "tmptest"

  private var scopeId: UUID = _
  private var userId: UUID = _
  private var grantedPermission1: PermissionToRead = _
  private var grantedPermission2: PermissionToRead = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    (for {
      scope ← EitherT(scopeDao.create(
        name = scopeName,
        parentId = None,
        description = None,
        createdBy = defaultCreatedBy,
        reactivate = false))
      user ← EitherT(backOfficeUserDao.create(
        userName = "user",
        password = "20202020",
        email = Email("m.zhyk@foo.bar"),
        phoneNumber = None,
        firstName = "Mikhail",
        middleName = None,
        lastName = "Zhyk",
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        roleId = initialData.defaultRoleId,
        businessUnitId = initialData.defaultBusinessUnitId,
        createdBy = defaultCreatedBy,
        reactivate = false))
    } yield {
      scopeId = scope.id
      userId = user.id
    }).value
      .unsafeRunSync()
      .leftMap(err ⇒ throw new RuntimeException(s"Data was not initialized properly: $err"))
      .merge
  }

  "Permission routes" should {
    "grant new permissions" in {
      // permission 1
      val pToCreate1 = PermissionToCreate(
        permissionKey = PermissionKeys.BusinessUnitAndRolePermissionKey(
          buId = initialData.defaultBusinessUnitId,
          roleId = initialData.defaultRoleId),
        revoke = None,
        scopeId = scopeId)
      val resp1 = services.run(buildPostRequest[PermissionToCreate](
        uri = baseRoute,
        entity = pToCreate1,
        token = initialData.superAdminToken))
      check[PermissionToRead](resp1, Status.Created, p ⇒ {
        grantedPermission1 = p
        grantedPermission1.scope.id mustBe pToCreate1.scopeId
      })
      // permission 2
      val pToCreate2 = PermissionToCreate(
        permissionKey = PermissionKeys.UserPermissionKey(userId = userId),
        revoke = Some(true),
        scopeId = scopeId)
      val resp2 = services.run(buildPostRequest[PermissionToCreate](
        uri = baseRoute,
        entity = pToCreate2,
        token = initialData.superAdminToken))
      check[PermissionToRead](resp2, Status.Created, p ⇒ {
        grantedPermission2 = p
        grantedPermission2.scope.id mustBe pToCreate2.scopeId
      })
      // reactivate permission 1
      val reactivateRoute = baseRoute + "?reactivate=true"
      val resp3 = services.run(buildPostRequest[PermissionToCreate](
        uri = reactivateRoute,
        entity = pToCreate1,
        token = initialData.superAdminToken))
      check[PermissionToRead](resp3, Status.Created, p ⇒ {
        p.id mustBe grantedPermission1.id
      })
    }

    "find permissions by permission key params" in {
      val buId = initialData.defaultBusinessUnitId
      val rId = initialData.defaultRoleId
      val uri1 = baseRoute + s"?business_unit_id=$buId&role_id=$rId"
      val resp1 = services.run(buildGetRequest(
        uri = uri1,
        token = initialData.superAdminToken))
      check[PaginatedResult[PermissionToRead]](resp1, Status.Ok, permissions ⇒ {
        permissions.results.size mustBe 1
        permissions.total mustBe 1
      })
      val uri2 = uri1 + s"&user_id=$userId"
      val resp2 = services.run(buildGetRequest(
        uri = uri2,
        token = initialData.superAdminToken))
      check[PaginatedResult[PermissionToRead]](resp2, Status.Ok, permissions ⇒ {
        permissions.results.isEmpty mustBe true
        permissions.total mustBe 0
      })
      val uri3 = uri1 + s"&limit=1"
      val resp3 = services.run(buildGetRequest(
        uri = uri3,
        token = initialData.superAdminToken))
      check[PaginatedResult[PermissionToRead]](resp3, Status.Ok, permissions ⇒ {
        permissions.results.size mustBe 1
        permissions.total mustBe 1
      })
      val uri4 = uri3 + s"&offset=1"
      val resp4 = services.run(buildGetRequest(
        uri = uri4,
        token = initialData.superAdminToken))
      check[PaginatedResult[PermissionToRead]](resp4, Status.Ok, permissions ⇒ {
        permissions.results.isEmpty mustBe true
        permissions.total mustBe 1
      })
    }

    "update permission by id" in {
      val buToUpdate = PermissionToUpdate(
        permissionKey = Some(UserPermissionKey(userId = userId)),
        scopeId = Some(scopeId))
      val request = buildPutRequest[PermissionToUpdate](
        uri = baseRoute + s"/${grantedPermission2.id}",
        entity = buToUpdate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[PermissionToRead](resp, Status.Ok, p ⇒ {
        p.scope.name mustBe grantedPermission2.scope.name
      })
    }

    "delete permission by id" in {
      val buId = initialData.defaultBusinessUnitId
      val rId = initialData.defaultRoleId
      val resp = for {
        request1 ← IO(buildDeleteRequest(
          uri = baseRoute + s"/${grantedPermission1.id}",
          token = initialData.superAdminToken))
        _ ← services.run(request1)
        request2 ← IO(buildGetRequest(
          uri = baseRoute + s"?business_unit_id=$buId&role_id=$rId",
          token = initialData.superAdminToken))
        resp2 ← services.run(request2)
      } yield resp2
      check[PaginatedResult[PermissionToRead]](resp, Status.Ok, _.results.isEmpty mustBe true)
    }
  }

}
