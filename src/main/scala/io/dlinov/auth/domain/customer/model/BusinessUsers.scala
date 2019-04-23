package io.dlinov.auth.domain.customer.model

import java.time.LocalDateTime
import java.util.UUID

import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import CustomerAttributes._
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Email}

object BusinessUsers {
  case class ActivatedBusinessUser(
      id: UUID, username: String,
      hashedPassword: Option[String],
      tier: CustomerTier,
      segment: Option[CustomerSegment],
      subscription: CustomerSubscription,
      emails: Set[Email],
      status: CustomerStatus,
      msisdn: Option[Msisdn],
      businessUserType: Option[BusinessUserType],

      company: NameAttribute,
      addresses: Set[Address],
      phoneNumbers: Set[PhoneNumber],
      activationRequirements: Set[ActivationRequirement],

      accounts: Set[Account],
      activatedAt: Option[LocalDateTime],
      passwordUpdatedAt: Option[LocalDateTime], createdAt: LocalDateTime,
      createdBy: BackOfficeUser, updatedAt: Option[LocalDateTime], updatedBy: Option[BackOfficeUser]) {
    assert(addresses.nonEmpty)
    assert(emails.nonEmpty)
    assert(phoneNumbers.nonEmpty)
    assert(activationRequirements.nonEmpty)
  }

  case class RegisteredButNotActivatedBusinessUser(
      id: UUID, username: String, email: Email,
      msisdn: Option[Msisdn], company: NameAttribute,
      account: Account,
      createdAt: LocalDateTime,
      createdBy: BackOfficeUser)
}

