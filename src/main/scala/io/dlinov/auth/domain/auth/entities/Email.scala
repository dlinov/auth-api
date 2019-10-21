package io.dlinov.auth.domain.auth.entities

final case class Email(value: String) {
  def isValid: Boolean = Email.isValid(value)
  def domain: String   = Email.domainRegex.findFirstMatchIn(value).map(_.group(1)).getOrElse("")
}

object Email {

  def createValidated(value: String): Email = {
    assert(isValid(value), s"$value is not a valid email")
    Email(value)
  }

  def isValid(value: String): Boolean = {
    Email.validationRegex.findFirstMatchIn(value).isDefined
  }

  // RFC 5322
  final private val validationRegex =
    ("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"" +
      "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
      "@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|" +
      "[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c" +
      "\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])").r

  final private val domainRegex = "^.*@(.*)$".r
}
