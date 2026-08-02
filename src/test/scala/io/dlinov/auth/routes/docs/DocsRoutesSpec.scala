package io.dlinov.auth.routes.docs

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.{Method, Request, Status, Uri}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class DocsRoutesSpec extends AnyWordSpecLike with Matchers {

  "ApiEndpoints.all" should {
    "cover every live route class with at least one documented endpoint" in {
      val templates = ApiEndpoints.all.map(e => (e.method, e.showPathTemplate()))
      templates must contain((Some(sttp.model.Method.GET), "/health"))
      templates must contain((Some(sttp.model.Method.POST), "/api/login"))
      templates must contain((Some(sttp.model.Method.GET), "/api/status"))
      templates must contain((Some(sttp.model.Method.GET), "/api/validate_token"))
      templates must contain((Some(sttp.model.Method.POST), "/api/admin/reset_password"))
      templates must contain(
        (Some(sttp.model.Method.POST), "/api/scopes?reactivate={reactivate}")
      )
      templates must contain(
        (Some(sttp.model.Method.GET), "/api/scopes?limit={limit}&offset={offset}")
      )
      templates must contain((Some(sttp.model.Method.GET), "/api/scopes/{id}"))
      templates must contain((Some(sttp.model.Method.PUT), "/api/scopes/{id}"))
      templates must contain((Some(sttp.model.Method.DELETE), "/api/scopes/{id}"))
      templates must contain(
        (Some(sttp.model.Method.POST), "/api/roles?reactivate={reactivate}")
      )
      templates must contain(
        (Some(sttp.model.Method.POST), "/api/business_units?reactivate={reactivate}")
      )
      templates must contain(
        (Some(sttp.model.Method.POST), "/api/back_office_users?reactivate={reactivate}")
      )
      // back_office_users' paginated listing additionally documents first_name/last_name/
      // email/phone_number filters (unlike scopes/roles/business_units, which only have
      // limit/offset) - on the same single GET endpoint, not a separate/duplicate one.
      val backOfficeUserListings = templates.collect {
        case (Some(sttp.model.Method.GET), path) if path.startsWith("/api/back_office_users?") =>
          path
      }
      backOfficeUserListings.size mustBe 1
      val backOfficeUserListing = backOfficeUserListings.head
      backOfficeUserListing must include("first_name={first_name}")
      backOfficeUserListing must include("last_name={last_name}")
      backOfficeUserListing must include("email={email}")
      backOfficeUserListing must include("phone_number={phone_number}")
      backOfficeUserListing must include("limit={limit}")
      backOfficeUserListing must include("offset={offset}")
      // exactly one GET /api/back_office_users?... endpoint - no duplicate method/path definition
      templates.count { case (method, path) =>
        method.contains(sttp.model.Method.GET) && path.startsWith("/api/back_office_users")
      } mustBe 2 // the paginated listing above, plus the separate GET .../{id} lookup
      templates.exists { case (method, path) =>
        method.contains(sttp.model.Method.GET) && path.startsWith("/api/permissions?")
      } mustBe true
      templates must contain((Some(sttp.model.Method.GET), "/api/accounts/*"))
      templates must contain((Some(sttp.model.Method.GET), "/api/transactions/*"))
      templates must contain((Some(sttp.model.Method.GET), "/api/types/*"))
      templates must contain((Some(sttp.model.Method.GET), "/api/currencies/*"))
      templates must contain((Some(sttp.model.Method.GET), "/api/account_types/*"))
      // types/currencies/account_types are read-only reverse proxies: no mutating methods
      templates.count(_._2 == "/api/types/*") mustBe 1
    }
  }

  "DocsRoutes" should {
    val docsRoutes = new DocsRoutes[IO]

    "serve the Swagger UI at /docs" in {
      val resp = docsRoutes.routes
        .run(Request[IO](Method.GET, Uri.unsafeFromString("/docs")))
        .value
        .unsafeRunSync()
      // Swagger UI's index page is served with a redirect from the bare /docs path to
      // /docs/index.html?...; either a direct 200 or a redirect both prove the docs route is
      // mounted and responding.
      resp.map(_.status.responseClass.isSuccess).getOrElse(false) mustBe true
    }

    "serve the generated OpenAPI yaml at /docs/docs.yaml, listing the documented paths" in {
      val resp = docsRoutes.routes
        .run(Request[IO](Method.GET, Uri.unsafeFromString("/docs/docs.yaml")))
        .value
        .unsafeRunSync()
      resp.map(_.status) mustBe Some(Status.Ok)
      val body = resp.get.body.through(fs2.text.utf8.decode).compile.string.unsafeRunSync()
      body must include("/api/scopes")
      body must include("/api/login")
      body must include("/api/types")
    }
  }
}
