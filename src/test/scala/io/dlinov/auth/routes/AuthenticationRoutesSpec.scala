package io.dlinov.auth.routes

import cats.effect.IO
import io.circe.Json
import io.dlinov.auth.domain.ErrorCodes.ValidationFailed
import io.dlinov.auth.routes.dto.{ApiError, BackOfficeUserToCreate, BackOfficeUserToRead, CredentialsToRead, CredentialsToUpdate, LoginResponse, LoginStatusResponse, PasswordReset, ResetPasswordLinkRequest}
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.json.EntityDecoders.{apiErrorEntityDecoder, bouToReadEntityDecoder, loginResponseEntityDecoder, loginStatusResponseEntityDecoder}
import io.dlinov.auth.routes.json.EntityEncoders.{credentialsToReadEntityEncoder, credentialsToUpdateEntityEncoder, passwordLinkRequestEntityEncoder, passwordResetEntityDecoder}
import org.http4s.Status

class AuthenticationRoutesSpec extends Http4sSpec {

  private val userName = "myappuser"
  private val userEmail = Email("myappuser@te.st")
  private var userPassword: String = _
  private var userToken: String = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    val userToCreate = BackOfficeUserToCreate(
      userName = userName,
      roleId = initialData.defaultRoleId,
      businessUnitId = initialData.defaultBusinessUnitId,
      email = userEmail,
      phoneNumber = Some("1234567"),
      firstName = "Kastus'",
      middleName = None,
      lastName = "Kalinouski",
      description = Some("revolutionery"),
      homePage = Some("https://staronka.by"),
      activeLanguage = Some("BY"),
      customData = Some(Json.obj("occupation" → Json.fromString("writer"))))
    registerUser(userToCreate, initialData.superAdminToken)
      .unsafeRunSync()
      .foreach(userPassword = _)
  }

  "Authentication routes" should {
    "provide login config (e.g. if captcha is enabled)" in {
      val request = buildGetRequest("/api/status")
      val resp = services.run(request)
      check[LoginStatusResponse](resp, Status.Ok, lsr ⇒ {
        lsr.requireCaptcha mustBe false
      })
    }

    "fail to login with empty user name" in {
      val credentials = CredentialsToRead("", userPassword, captcha = None)
      val request = buildPostRequest[CredentialsToRead]("/api/login", credentials)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "fail to login with empty password" in {
      val credentials = CredentialsToRead(userName, "", captcha = None)
      val request = buildPostRequest[CredentialsToRead]("/api/login", credentials)
      val resp = services.run(request)
      check(resp, Status.UnprocessableEntity)
    }

    "login with valid credentials" in {
      val credentials = CredentialsToRead(userName, userPassword, captcha = None)
      val request = buildPostRequest[CredentialsToRead]("/api/login", credentials)
      val resp = services.run(request)
      check[LoginResponse](resp, Status.Ok, r ⇒ {
        userToken = r.token
        r.token.nonEmpty mustBe true
        r.user.userName mustBe userName
      })
    }

    "validate received token" in {
      val request = buildGetRequest("/api/validate_token", userToken)
      val resp = services.run(request)
      check[BackOfficeUserToRead](resp, Status.Ok, user ⇒ {
        user.userName mustBe userName
      })
    }

    "reset password" in {
      val passwordResetEntity = ResetPasswordLinkRequest(
        userName = userName,
        email = userEmail,
        captcha = None)
      val latestNotification = notificationsBuffer.lastOption
      val baseUrl = "/api/reset_password"
      val passwordRegex = "(/reset_password\\?token=(.*))\\.".r
      val newPassword = "Qwerty123!"
      val resp = for {
        // request for password reset link in email
        request1 ← IO(buildPostRequest[ResetPasswordLinkRequest](baseUrl, passwordResetEntity))
        _ ← services.run(request1)
        // parse password reset link from email
        pwdResetLinkAndToken ← IO {
          (for {
            pwdResetNotification ← fetchNextNotification(latestNotification)
            pwdResetLinkMatch ← passwordRegex.findFirstMatchIn(pwdResetNotification.message)
          } yield ("/api" + pwdResetLinkMatch.group(1)) → pwdResetLinkMatch.group(2)).get
        }
        (pwdResetLink, token) = pwdResetLinkAndToken
        // follow link from email
        request2 ← IO(buildGetRequest(pwdResetLink))
        _ ← services.run(request2)
        // ask specific new password
        request3 ← IO(buildPutRequest(baseUrl, PasswordReset(newPassword, token)))
        _ ← services.run(request3)
        // login with new password
        request4 ← IO {
          val credentials = CredentialsToRead(userName, newPassword, captcha = None)
          buildPostRequest[CredentialsToRead]("/api/login", credentials)
        }
        resp4 ← services.run(request4)
      } yield resp4
      check[LoginResponse](resp, Status.Ok, loginInfo ⇒ {
        userPassword = newPassword
        userToken = loginInfo.token
        loginInfo.user.userName mustBe userName
      })
    }

    "fail to update password" in {
      val newPassword = "LAYA1234!"
      val baseUrl = "/api/update_password"
      val credentials = CredentialsToUpdate(userName, userPassword, newPassword)
      val request = buildPutRequest[CredentialsToUpdate](baseUrl, credentials)
      val response = services.run(request)
      check[ApiError](response, Status.BadRequest, error ⇒ {
        error.code mustBe ValidationFailed
      })
    }

    "update password" in {
      val newPassword = "upd_4gaiN"
      val baseUrl = "/api/update_password"
      val credentials = CredentialsToUpdate(userName, userPassword, newPassword)
      val resp = for {
        // update password
        request1 ← IO(buildPutRequest[CredentialsToUpdate](baseUrl, credentials))
        _ ← services.run(request1)
        // login with new password
        request2 ← IO {
          val credentials = CredentialsToRead(userName, newPassword, captcha = None)
          buildPostRequest[CredentialsToRead]("/api/login", credentials)
        }
        resp2 ← services.run(request2)
      } yield resp2
      check[LoginResponse](resp, Status.Ok, loginInfo ⇒ {
        userPassword = newPassword
        userToken = loginInfo.token
        loginInfo.user.userName mustBe userName
      })
    }
  }
}
