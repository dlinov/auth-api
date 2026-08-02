package io.dlinov.auth.routes

import cats.data.EitherT
import cats.effect.unsafe.implicits.global
import io.dlinov.auth.TokenBehavior
import io.dlinov.auth.domain.auth.entities.{ClaimContent, Email}
import io.dlinov.auth.routes.json.CirceEncoders.claimContentEncoder
import org.http4s.Status

/** Covers requirement: TypesProxy (fronting `/api/types`, `/api/currencies`, `/api/account_types`)
  * must require authentication and the `types` read permission - there is no public/anonymous
  * wildcard proxy endpoint left in the service. The "allowed request forwards" case (once
  * authenticated and authorized) is covered separately in `TypesProxySpec`, against a stub
  * `Client`, since exercising a real downstream network call isn't meaningful/available in this
  * test environment.
  */
class TypesProxyAuthSpec extends Http4sSpec {

  private lazy val unprivilegedUserToken: String = {
    val created = (for {
      user <- EitherT(
        backOfficeUserDao.create(
          userName = "no-types-permission-user",
          password = "password",
          email = Email("no-types-permission-user@foo.bar"),
          phoneNumber = None,
          firstName = "No",
          middleName = None,
          lastName = "Permissions",
          description = None,
          homePage = None,
          activeLanguage = None,
          customData = None,
          roleId = initialData.defaultRoleId,
          businessUnitId = initialData.defaultBusinessUnitId,
          createdBy = defaultCreatedBy,
          reactivate = false
        )
      )
    } yield user).value.unsafeRunSync().toOption.get
    val gen = new TokenBehavior {
      override def tokenExpirationInMinutes: Int = 60
    }
    gen.generateTokenCirce("no-types-permission-user", ClaimContent.from(created))
  }

  List("/api/types", "/api/currencies", "/api/account_types").foreach { root =>
    s"GET $root" should {
      "return 401 Unauthorized when no bearer token is supplied" in {
        val resp = services.run(buildGetRequest(root))
        check(resp, Status.Unauthorized)
      }

      "return 403 Forbidden when the caller lacks the types permission" in {
        val resp = services.run(buildGetRequest(root, unprivilegedUserToken))
        check(resp, Status.Forbidden)
      }
    }
  }
}
