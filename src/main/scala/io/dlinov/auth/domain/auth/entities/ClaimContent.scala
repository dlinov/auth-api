package io.dlinov.auth.domain.auth.entities

final case class ClaimContent(
    loggedInAs: String,
    // excluded from header as cookie might overflow 4093 bytes (max browser supported cookie size)
    /*permissions: Seq[Permission],*/
    email: Email
)

object ClaimContent {
  def from(user: BackOfficeUser): ClaimContent = {
    ClaimContent(loggedInAs = user.userName, email = user.email)
  }
}
