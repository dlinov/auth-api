package io.dlinov.auth.routes

import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.routes.dto.{ScopeToCreate, ScopeToRead, ScopeToUpdate}
import org.http4s.Status
import io.dlinov.auth.routes.dto.{ScopeToCreate, ScopeToRead, ScopeToUpdate}

class ScopeRouteSpec extends Http4sSpec {

  private val baseRoute = "/api/scopes"

  private var createdScope: ScopeToRead = _
  private var scopeId: UUID             = _

  "Scope routes" should {

    "fail to create new scope with empty name" in {
      val scopeToCreate = ScopeToCreate(name = "", parentId = None)
      val request = buildPostRequest[ScopeToCreate](
        uri = baseRoute,
        entity = scopeToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create new scope with too long name" in {
      val scopeToCreate = ScopeToCreate(name = "abc" * 11, parentId = None)
      val request = buildPostRequest[ScopeToCreate](
        uri = baseRoute,
        entity = scopeToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create new scope with empty string description" in {
      val scopeToCreate = ScopeToCreate(name = "scopetest", parentId = None, description = Some(""))
      val request = buildPostRequest[ScopeToCreate](
        uri = baseRoute,
        entity = scopeToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create new scope with too long description" in {
      val scopeToCreate =
        ScopeToCreate(name = "scopetest", parentId = None, description = Some("x" * 256))
      val request = buildPostRequest[ScopeToCreate](
        uri = baseRoute,
        entity = scopeToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create new scope with non-existing parent id" in {
      val scopeToCreate = ScopeToCreate(name = "scopetest", parentId = Some(UUID.randomUUID()))
      val request = buildPostRequest[ScopeToCreate](
        uri = baseRoute,
        entity = scopeToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.InternalServerError) // probably BadRequest is better here
    }

    "create new scope" in {
      val scopeToCreate = ScopeToCreate(name = "scopetest", parentId = None)
      val request = buildPostRequest[ScopeToCreate](
        uri = baseRoute,
        entity = scopeToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[ScopeToRead](resp, Status.Created, bu ⇒ {
        createdScope = bu
        scopeId = createdScope.id
        createdScope.name mustBe scopeToCreate.name
      })
    }

    "create new child scope" in {
      val scopeToCreate = ScopeToCreate(name = "scopetest-child", parentId = Some(scopeId))
      val request = buildPostRequest[ScopeToCreate](
        uri = baseRoute,
        entity = scopeToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[ScopeToRead](resp, Status.Created, bu ⇒ {
        bu.name mustBe scopeToCreate.name
        bu.parentId.contains(scopeId) mustBe true
      })
    }

    "find scope by id" in {
      val request =
        buildGetRequest(uri = baseRoute + s"/$scopeId", token = initialData.superAdminToken)
      val resp = services.run(request)
      check[ScopeToRead](resp, Status.Ok, r ⇒ {
        createdScope.name mustBe r.name
      })
    }

    "update scope by id" in {
      val scopeToUpdate = ScopeToUpdate(description = Some("fresh new description"))
      val request = buildPutRequest[ScopeToUpdate](
        uri = baseRoute + s"/$scopeId",
        entity = scopeToUpdate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[ScopeToRead](resp, Status.Ok, r ⇒ {
        createdScope = r
        scopeToUpdate.description mustBe createdScope.description
      })
    }

    "delete scope by id" in {
      val resp = for {
        request1 ← IO(
          buildDeleteRequest(uri = baseRoute + s"/$scopeId", token = initialData.superAdminToken)
        )
        _ ← services.run(request1)
        request2 ← IO(
          buildGetRequest(uri = baseRoute + s"/$scopeId", token = initialData.superAdminToken)
        )
        resp2 ← services.run(request2)
      } yield resp2
      check(resp, Status.NotFound)
    }
  }

}
