package io.dlinov.auth.routes

import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.{RoleToCreate, RoleToRead, RoleToUpdate}
import org.http4s.Status
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.{RoleToCreate, RoleToRead, RoleToUpdate}
import io.dlinov.auth.routes.json.EntityDecoders.{rolePageEntityDecoder, roleToReadEntityDecoder}
import io.dlinov.auth.routes.json.EntityEncoders.{roleToCreateEntityEncoder, roleToUpdateEntityEncoder}

class RoleRoutesSpec extends Http4sSpec {

  private val baseRoute = "/api/roles"

  private var createdRole: RoleToRead = _
  private var rId: UUID = _

  "Role routes" should {
    "fail to create a new role with empty name" in {
      val roleToCreate = RoleToCreate(name = "")
      val request = buildPostRequest[RoleToCreate](
        uri = baseRoute,
        entity = roleToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create a new role with too long name" in {
      val roleToCreate = RoleToCreate(name = "a" * 129)
      val request = buildPostRequest[RoleToCreate](
        uri = baseRoute,
        entity = roleToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "create a new role" in {
      val roleToCreate = RoleToCreate(name = "new role")
      val request = buildPostRequest[RoleToCreate](
        uri = baseRoute,
        entity = roleToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[RoleToRead](resp, Status.Created, bu ⇒ {
        createdRole = bu
        rId = createdRole.id
        createdRole.name mustBe roleToCreate.name
      })
    }

    "find role by id" in {
      val request = buildGetRequest(
        uri = baseRoute + s"/$rId",
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[RoleToRead](resp, Status.Ok, r ⇒ {
        createdRole.name mustBe r.name
      })
    }

    "find all roles" in {
      val request = buildGetRequest(
        uri = baseRoute,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[PaginatedResult[RoleToRead]](resp, Status.Ok, page ⇒ {
        val roles = page.results
        roles.exists(_.name == createdRole.name) mustBe true
      })
    }

    "update role by id" in {
      val roleToUpdate = RoleToUpdate(name = "updated")
      val request = buildPutRequest[RoleToUpdate](
        uri = baseRoute + s"/$rId",
        entity = roleToUpdate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[RoleToRead](resp, Status.Ok, r ⇒ {
        createdRole = r
        roleToUpdate.name mustBe createdRole.name
      })
    }

    "delete role by id" in {
      val resp = for {
        request1 ← IO(buildDeleteRequest(uri = baseRoute + s"/$rId", token = initialData.superAdminToken))
        _ ← services.run(request1)
        request2 ← IO(buildGetRequest(
          uri = baseRoute + s"/$rId",
          token = initialData.superAdminToken))
        resp2 ← services.run(request2)
      } yield resp2
      check(resp, Status.NotFound)
    }
  }

}
