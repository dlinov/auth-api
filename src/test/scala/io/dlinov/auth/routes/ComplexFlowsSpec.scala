package io.dlinov.auth.routes

import java.util.UUID

import cats.effect.IO
import io.circe.{parser, Json}
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.dto.{
  BackOfficeUserToCreate,
  BackOfficeUserToRead,
  BackOfficeUserToUpdate,
  BusinessUnitToCreate,
  BusinessUnitToRead,
  BusinessUnitToUpdate,
  RoleToCreate,
  RoleToRead,
  RoleToUpdate
}
import org.http4s.Status
import org.http4s.circe.jsonEncoder
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.dto.{
  BackOfficeUserToCreate,
  BackOfficeUserToRead,
  BackOfficeUserToUpdate,
  BusinessUnitToCreate,
  BusinessUnitToRead,
  BusinessUnitToUpdate,
  RoleToCreate,
  RoleToRead,
  RoleToUpdate
}

class ComplexFlowsSpec extends Http4sSpec {

  private val buRoute    = "/api/business_units"
  private val rolesRoute = "/api/roles"
  private val bouRoute   = "/api/back_office_users"
  private val emptyUuid  = new UUID(0L, 0L)

  private var createdBusinessUnit: BusinessUnitToRead = _
  private var buId: UUID                              = _
  private var createdRole: RoleToRead                 = _
  private var rId: UUID                               = _
  private var userId: UUID                            = _
  private var createdUser: BackOfficeUserToRead       = _

  "BackOfficeUser routes pt. I" should {
    "fail to create new backoffice user with non-existing business unit id" in {
      val userToCreate = BackOfficeUserToCreate(
        userName = "user_test_1",
        email = Email("user1@te.st"),
        phoneNumber = Some("1234567"),
        firstName = "Nikifor",
        middleName = None,
        lastName = "Romaschenko",
        description = None,
        homePage = Some("https://sports.ru"),
        activeLanguage = None,
        customData = None,
        roleId = initialData.defaultRoleId,
        businessUnitId = emptyUuid
      )
      val request = buildPostRequest[BackOfficeUserToCreate](
        uri = bouRoute,
        entity = userToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.NotFound)
    }

    "fail to create new backoffice user with non-existing role id" in {
      val userToCreate = BackOfficeUserToCreate(
        userName = "user_test_1",
        email = Email("user1@te.st"),
        phoneNumber = Some("1234567"),
        firstName = "Nikifor",
        middleName = None,
        lastName = "Romaschenko",
        description = None,
        homePage = Some("https://sports.ru"),
        activeLanguage = None,
        customData = None,
        roleId = initialData.defaultRoleId,
        businessUnitId = emptyUuid
      )
      val request = buildPostRequest[BackOfficeUserToCreate](
        uri = bouRoute,
        entity = userToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.NotFound)
    }
  }

  "BusinessUnit routes" should {
    "fail to create new business unit with empty name" in {
      val buToCreate = BusinessUnitToCreate(name = "")
      val request = buildPostRequest[BusinessUnitToCreate](
        uri = buRoute,
        entity = buToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create new business unit with too long name" in {
      val buToCreate = BusinessUnitToCreate(name = "n3w" * 11)
      val request = buildPostRequest[BusinessUnitToCreate](
        uri = buRoute,
        entity = buToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "create new business unit" in {
      val buToCreate = BusinessUnitToCreate(name = "new-Bus1n355_UNIT .")
      val request = buildPostRequest[BusinessUnitToCreate](
        uri = buRoute,
        entity = buToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[BusinessUnitToRead](resp, Status.Created, bu ⇒ {
        createdBusinessUnit = bu
        buId = createdBusinessUnit.id
        createdBusinessUnit.name mustBe buToCreate.name
      })
    }

    "find business unit by id" in {
      val request = buildGetRequest(uri = buRoute + s"/$buId", token = initialData.superAdminToken)
      val resp    = services.run(request)
      check[BusinessUnitToRead](resp, Status.Ok, bu ⇒ {
        createdBusinessUnit.name mustBe bu.name
      })
    }

    "find all business units" in {
      val request = buildGetRequest(uri = buRoute, token = initialData.superAdminToken)
      val resp    = services.run(request)
      check[PaginatedResult[BusinessUnitToRead]](resp, Status.Ok, page ⇒ {
        val bUnits = page.results
        bUnits.size mustBe page.total
        bUnits.exists(_.name == createdBusinessUnit.name) mustBe true
      })
    }

    "update business unit by id" in {
      val buToUpdate = BusinessUnitToUpdate(name = "updated")
      val request = buildPutRequest[BusinessUnitToUpdate](
        uri = buRoute + s"/$buId",
        entity = buToUpdate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[BusinessUnitToRead](resp, Status.Ok, bu ⇒ {
        createdBusinessUnit = bu
        buToUpdate.name mustBe createdBusinessUnit.name
      })
    }
  }

  "Role routes" should {
    "fail to create a new role with empty name" in {
      val roleToCreate = RoleToCreate(name = "")
      val request = buildPostRequest[RoleToCreate](
        uri = rolesRoute,
        entity = roleToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create a new role with too long name" in {
      val roleToCreate = RoleToCreate(name = "a" * 129)
      val request = buildPostRequest[RoleToCreate](
        uri = rolesRoute,
        entity = roleToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "create a new role" in {
      val roleToCreate = RoleToCreate(name = "new role")
      val request = buildPostRequest[RoleToCreate](
        uri = rolesRoute,
        entity = roleToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[RoleToRead](resp, Status.Created, bu ⇒ {
        createdRole = bu
        rId = createdRole.id
        createdRole.name mustBe roleToCreate.name
      })
    }

    "find role by id" in {
      val request =
        buildGetRequest(uri = rolesRoute + s"/$rId", token = initialData.superAdminToken)
      val resp = services.run(request)
      check[RoleToRead](resp, Status.Ok, r ⇒ {
        createdRole.name mustBe r.name
      })
    }

    "find all roles" in {
      val request = buildGetRequest(uri = rolesRoute, token = initialData.superAdminToken)
      val resp    = services.run(request)
      check[PaginatedResult[RoleToRead]](resp, Status.Ok, page ⇒ {
        val roles = page.results
        roles.exists(_.name == createdRole.name) mustBe true
      })
    }

    "update role by id" in {
      val roleToUpdate = RoleToUpdate(name = "updated")
      val request = buildPutRequest[RoleToUpdate](
        uri = rolesRoute + s"/$rId",
        entity = roleToUpdate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[RoleToRead](resp, Status.Ok, r ⇒ {
        createdRole = r
        roleToUpdate.name mustBe createdRole.name
      })
    }
  }

  "BackOfficeUser routes pt. II" should {
    "fail to create new backoffice user when name is invalid" in {
      val json = parser
        .parse(
          s"""{"role":"$rId","business_unit":"$buId","first_name":"Daniel","user_name":"daniel,icardo","last_name":"Icardo","manual_user_name":"daniel.icardo","phone_number":"971787878782","email":"d.icardo@foo.bar","business_unit_id":"09864604-2caf-4b3f-a79c-c841105f580b","role_id":"12ec03f3-d1dc-11e8-bcd3-000c291e73b1"}"""
        )
        .getOrElse(Json.Null)
      val request =
        buildPostRequest[Json](uri = bouRoute, entity = json, token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "create new backoffice user" in {
      val userToCreate = BackOfficeUserToCreate(
        userName = "user_test_1",
        email = Email("user1@te.st"),
        phoneNumber = Some("1234567"),
        firstName = "Nikifor",
        middleName = None,
        lastName = "Romaschenko",
        description = None,
        homePage = Some("https://sports.ru"),
        activeLanguage = None,
        customData = None,
        roleId = rId,
        businessUnitId = buId
      )
      val request = buildPostRequest[BackOfficeUserToCreate](
        uri = bouRoute,
        entity = userToCreate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[BackOfficeUserToRead](resp, Status.Created, user ⇒ {
        createdUser = user
        userId = createdUser.id
        createdUser.userName mustBe userToCreate.userName
      })
    }

    "find backoffice user by id" in {
      val request =
        buildGetRequest(uri = bouRoute + s"/$userId", token = initialData.superAdminToken)
      val resp = services.run(request)
      check[BackOfficeUserToRead](resp, Status.Ok, user ⇒ {
        createdUser.userName mustBe user.userName
      })
    }

    "find all backoffice users" in {
      val request = buildGetRequest(uri = bouRoute, token = initialData.superAdminToken)
      val resp    = services.run(request)
      check[PaginatedResult[BackOfficeUserToRead]](resp, Status.Ok, page ⇒ {
        val users = page.results
        users.size mustBe page.total
        users.exists(_.userName == createdUser.userName) mustBe true
      })
    }

    "find all backoffice users (with limit and offset)" in {
      val request =
        buildGetRequest(uri = bouRoute + "?limit=1&offset=1", token = initialData.superAdminToken)
      val resp = services.run(request)
      check[PaginatedResult[BackOfficeUserToRead]](resp, Status.Ok, users ⇒ {
        users.total mustBe 2
        users.results.size mustBe 1
      })(bouPageEntityDecoder)
    }

    "update backoffice user by id" in {
      val userToUpdate = BackOfficeUserToUpdate.empty.copy(firstName = Some("Miroslav"))
      val request = buildPutRequest[BackOfficeUserToUpdate](
        uri = bouRoute + s"/$userId",
        entity = userToUpdate,
        token = initialData.superAdminToken
      )
      val resp = services.run(request)
      check[BackOfficeUserToRead](resp, Status.Ok, user ⇒ {
        createdUser = user
        userId = createdUser.id
        userToUpdate.firstName.contains(createdUser.firstName) mustBe true
      })
    }
  }

  "BusinessUnit routes pt. II" should {
    "fail to delete business unit with active users" in {
      val request =
        buildDeleteRequest(uri = buRoute + s"/$buId", token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.BadRequest)
    }
  }

  "Role routes pt. II" should {
    "fail to delete role with active users" in {
      val request =
        buildDeleteRequest(uri = rolesRoute + s"/$rId", token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.BadRequest)
    }
  }

  "BackOfficeUser routes pt. III" should {
    "delete backoffice user by id" in {
      val resp = for {
        request1 ← IO(
          buildDeleteRequest(uri = bouRoute + s"/$userId", token = initialData.superAdminToken)
        )
        _ ← services.run(request1)
        request2 ← IO(
          buildGetRequest(uri = bouRoute + s"/$userId", token = initialData.superAdminToken)
        )
        resp2 ← services.run(request2)
      } yield resp2
      check(resp, Status.NotFound)
    }
  }

  "BusinessUnit routes pt. III" should {
    "delete business unit by id" in {
      val resp = for {
        request1 ← IO(
          buildDeleteRequest(uri = buRoute + s"/$buId", token = initialData.superAdminToken)
        )
        _ ← services.run(request1)
        request2 ← IO(
          buildGetRequest(uri = buRoute + s"/$buId", token = initialData.superAdminToken)
        )
        resp2 ← services.run(request2)
      } yield resp2
      check(resp, Status.NotFound)
    }
  }

  "Role routes pt. III" should {
    "delete role by id" in {
      val resp = for {
        request1 ← IO(
          buildDeleteRequest(uri = rolesRoute + s"/$rId", token = initialData.superAdminToken)
        )
        _ ← services.run(request1)
        request2 ← IO(
          buildGetRequest(uri = rolesRoute + s"/$rId", token = initialData.superAdminToken)
        )
        resp2 ← services.run(request2)
      } yield resp2
      check(resp, Status.NotFound)
    }
  }

}
