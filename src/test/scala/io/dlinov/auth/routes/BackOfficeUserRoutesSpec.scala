package io.dlinov.auth.routes

import java.util.UUID

import cats.effect.IO
import io.circe.{Json, parser}
import io.dlinov.auth.domain.ErrorCodes.ValidationFailed
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.dto.{ApiError, BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate}
import io.dlinov.auth.routes.json.EntityDecoders
import io.dlinov.auth.routes.json.EntityDecoders.{bouPageEntityDecoder, bouToReadEntityDecoder}
import io.dlinov.auth.routes.json.EntityEncoders.{backOfficeUserToCreateEntityEncoder, backOfficeUserToUpdateEntityEncoder}
import org.http4s._
import org.http4s.circe.jsonEncoder

class BackOfficeUserRoutesSpec extends Http4sSpec {

  private val baseRoute = "/api/back_office_users"

  private var userId: UUID = _
  private var createdUser: BackOfficeUserToRead = _

  "BackOfficeUser routes" should {
    "fail to create new backoffice user when name is invalid" in {
      val json = parser.parse(s"""{"role":"${initialData.defaultRoleId}","business_unit":"${initialData.defaultBusinessUnitId}","first_name":"Daniel","user_name":"daniel,icardo","last_name":"Icardo","manual_user_name":"daniel.icardo","phone_number":"971787878782","email":"d.icardo@foo.bar","business_unit_id":"09864604-2caf-4b3f-a79c-c841105f580b","role_id":"12ec03f3-d1dc-11e8-bcd3-000c291e73b1"}""")
        .getOrElse(Json.Null)
      val request = buildPostRequest[Json](
        uri = baseRoute,
        entity = json,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[ApiError](resp, Status.UnprocessableEntity, apiError ⇒ {
        assert(apiError.code == ValidationFailed)
        assert(apiError.msg.startsWith("Errors:"))
      })(EntityDecoders.apiErrorEntityDecoder)
    }

    "fail to create new backoffice user when email is too long" in {
      val userToCreate = BackOfficeUserToCreate(
        userName = "user_test_1",
        email = Email(s"${"user" * 15}@te.st"),
        phoneNumber = Some("1234567"),
        firstName = "Nikifor",
        middleName = None,
        lastName = "Romaschenko",
        description = None,
        homePage = Some("https://sports.ru"),
        activeLanguage = None,
        customData = None,
        roleId = initialData.defaultRoleId,
        businessUnitId = initialData.defaultBusinessUnitId)
      val request = buildPostRequest[BackOfficeUserToCreate](
        uri = baseRoute,
        entity = userToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to create new backoffice user when email is invalid" in {
      val userToCreate = BackOfficeUserToCreate(
        userName = "user_test_1",
        email = Email(s"abc@hgf"),
        phoneNumber = Some("1234567"),
        firstName = "Nikifor",
        middleName = None,
        lastName = "Romaschenko",
        description = None,
        homePage = Some("https://sports.ru"),
        activeLanguage = None,
        customData = None,
        roleId = initialData.defaultRoleId,
        businessUnitId = initialData.defaultBusinessUnitId)
      val request = buildPostRequest[BackOfficeUserToCreate](
        uri = baseRoute,
        entity = userToCreate,
        token = initialData.superAdminToken)
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
        roleId = initialData.defaultRoleId,
        businessUnitId = initialData.defaultBusinessUnitId)
      val request = buildPostRequest[BackOfficeUserToCreate](
        uri = baseRoute,
        entity = userToCreate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[BackOfficeUserToRead](resp, Status.Created, user ⇒ {
        createdUser = user
        userId = createdUser.id
        createdUser.userName mustBe userToCreate.userName
      })
    }

    "find backoffice user by id" in {
      val request = buildGetRequest(
        uri = baseRoute + s"/$userId",
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[BackOfficeUserToRead](resp, Status.Ok, user ⇒ {
        createdUser.userName mustBe user.userName
      })
    }

    "find all backoffice users" in {
      val request = buildGetRequest(
        uri = baseRoute,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[PaginatedResult[BackOfficeUserToRead]](resp, Status.Ok, page ⇒ {
        val users = page.results
        users.size mustBe page.total
        users.exists(_.userName == createdUser.userName) mustBe true
      })
    }

    "find all backoffice users (with limit and offset)" in {
      val request = buildGetRequest(
        uri = baseRoute + "?limit=1&offset=1",
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[PaginatedResult[BackOfficeUserToRead]](resp, Status.Ok, users ⇒ {
        users.total mustBe 2
        users.results.size mustBe 1
      })(bouPageEntityDecoder)
    }

    "update backoffice user by id" in {
      val userToUpdate = BackOfficeUserToUpdate.empty.copy(firstName = Some("Miroslav"))
      val request = buildPutRequest[BackOfficeUserToUpdate](
        uri = baseRoute + s"/$userId",
        entity = userToUpdate,
        token = initialData.superAdminToken)
      val resp = services.run(request)
      check[BackOfficeUserToRead](resp, Status.Ok, user ⇒ {
        createdUser = user
        userId = createdUser.id
        userToUpdate.firstName.contains(createdUser.firstName) mustBe true
      })
    }

    "delete backoffice user by id" in {
      val resp = for {
        request1 ← IO(buildDeleteRequest(uri = baseRoute + s"/$userId", token = initialData.superAdminToken))
        _ ← services.run(request1)
        request2 ← IO(buildGetRequest(
          uri = baseRoute + s"/$userId",
          token = initialData.superAdminToken))
        resp2 ← services.run(request2)
      } yield resp2
      check(resp, Status.NotFound)
    }
  }
}
