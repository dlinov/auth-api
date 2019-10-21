package io.dlinov.auth.domain.customer.model

import io.dlinov.auth.util.Implicits._

object CustomerAttributes {

  case class LoginUsername(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\@\.\_]*"""))
  }

  case class NameAttribute(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\.\' ]*"""))
  }

  case class AccountNumber(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z0-9\- ]+"""))
  }

  case class AccountStatus(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""))
  }

  case class AccountType(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""))
  }

  case class CustomerTier(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""))
  }

  case class CustomerSegment(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""))
  }

  case class CustomerSubscription(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""))
  }

  case class CustomerStatus(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""))
  }

  case class Msisdn(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[\+]?[1-9][0-9]{10,14}"""))
  }

  case class BusinessUserType(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""))
  }

  case class Address(
      underlying: String,
      country: Option[String] = None,
      postalCode: Option[String] = None,
      city: Option[String] = None,
      municipality: Option[String] = None,
      district: Option[String] = None,
      province: Option[String] = None,
      state: Option[String] = None,
      building: Option[String] = None,
      street: Option[String] = None,
      village: Option[String] = None,
      room: Option[String] = None,
      lot: Option[String] = None,
      block: Option[String] = None,
      houseNum: Option[String] = None
  ) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z0-9]+[A-Za-z0-9\.\' ]*"""))
  }

  case class PhoneNumber(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[\+]?[0-9]+[0-9\- ]*"""))
  }

  case class ActivationDocumentType(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""))
  }

  case class ActivationRequirement(identifier: String, documentType: ActivationDocumentType) {
    assert(identifier.hasSomething)
    assert(identifier.matches("""[A-Za-z0-9]+[A-Za-z0-9\-\_ ]*"""))
  }

}
