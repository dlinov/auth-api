package io.dlinov.auth.routes

import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.{BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate}
import org.http4s.Status
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto.{BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate}
import io.dlinov.auth.routes.json.EntityEncoders.{businessUnitToCreateEntityEncoder, businessUnitToUpdateEntityEncoder}
import io.dlinov.auth.routes.json.EntityDecoders.{buPageEntityDecoder, buToReadEntityDecoder}

class BusinessUnitRoutesSpec extends Http4sSpec {

  private val baseRoute = "/api/business_units"

  private var createdBusinessUnit: BusinessUnitToRead = _
  private var buId: UUID = _

  "BusinessUnit routes" should {
    "fail to create new business unit with empty name" in {
      val buToCreate = BusinessUnitToCreate(name = "")
      val request = buildPostRequest[BusinessUnitToCreate](
        uri = baseRoute,
        entity = buToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create new business unit with too long name" in {
      val buToCreate = BusinessUnitToCreate(name = "n3w" * 11)
      val request = buildPostRequest[BusinessUnitToCreate](
        uri = baseRoute,
        entity = buToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "create new business unit" in {
      val buToCreate = BusinessUnitToCreate(name = "new-Bus1n355_UNIT .")
      val request = buildPostRequest[BusinessUnitToCreate](
        uri = baseRoute,
        entity = buToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[BusinessUnitToRead](resp, Status.Created, bu ⇒ {
        createdBusinessUnit = bu
        buId = createdBusinessUnit.id
        createdBusinessUnit.name mustBe buToCreate.name
      })
    }

    "find business unit by id" in {
      val request = buildGetRequest(
        uri = baseRoute + s"/$buId",
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[BusinessUnitToRead](resp, Status.Ok, bu ⇒ {
        createdBusinessUnit.name mustBe bu.name
      })
    }

    "find all business units" in {
      val request = buildGetRequest(
        uri = baseRoute,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[PaginatedResult[BusinessUnitToRead]](resp, Status.Ok, page ⇒ {
        val bUnits = page.results
        bUnits.size mustBe page.total
        bUnits.exists(_.name == createdBusinessUnit.name) mustBe true
      })
    }

    "update business unit by id" in {
      val buToUpdate = BusinessUnitToUpdate(name = "updated")
      val request = buildPutRequest[BusinessUnitToUpdate](
        uri = baseRoute + s"/$buId",
        entity = buToUpdate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[BusinessUnitToRead](resp, Status.Ok, bu ⇒ {
        createdBusinessUnit = bu
        buToUpdate.name mustBe createdBusinessUnit.name
      })
    }

    "delete business unit by id" in {
      val resp = for {
        request1 ← IO(buildDeleteRequest(uri = baseRoute + s"/$buId", token = initialData.superAdminToken))
        _ ← services.run(request1)
        request2 ← IO(buildGetRequest(
          uri = baseRoute + s"/$buId",
          token = initialData.superAdminToken))
        resp2 ← services.run(request2)
      } yield resp2
      check(resp, Status.NotFound)
    }
  }

}
