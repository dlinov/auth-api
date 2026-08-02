package io.dlinov.auth.routes

import org.http4s.Uri
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class RoutesSpec extends AnyWordSpecLike with Matchers {

  private def path(raw: String): Uri.Path = Uri.unsafeFromString(raw).path

  "Routes.isPathUnderRoot" should {
    "match the root path itself" in {
      Routes.isPathUnderRoot("api/accounts")(path("/api/accounts")) mustBe true
    }

    "match descendants of the root path" in {
      Routes.isPathUnderRoot("api/accounts")(path("/api/accounts/123")) mustBe true
      Routes.isPathUnderRoot("api/accounts")(path("/api/accounts/123/sub")) mustBe true
    }

    "not match a sibling path that merely shares a textual prefix" in {
      // regression: raw String.startsWith previously matched "/api/accountsXYZ" against
      // root "api/accounts" because "/api/accountsXYZ".startsWith("/api/accounts") is true,
      // even though "accountsXYZ" is a different path segment than "accounts".
      Routes.isPathUnderRoot("api/accounts")(path("/api/accountsXYZ")) mustBe false
      Routes.isPathUnderRoot("api/accounts")(path("/api/accountsXYZ/123")) mustBe false
    }

    "not match unrelated paths" in {
      Routes.isPathUnderRoot("api/accounts")(path("/api/transactions")) mustBe false
      Routes.isPathUnderRoot("api/accounts")(path("/api")) mustBe false
      Routes.isPathUnderRoot("api/accounts")(path("/")) mustBe false
    }

    "work the same way for single-segment roots" in {
      Routes.isPathUnderRoot("api")(path("/api")) mustBe true
      Routes.isPathUnderRoot("api")(path("/api/accounts")) mustBe true
      Routes.isPathUnderRoot("api")(path("/apiary")) mustBe false
    }
  }
}
