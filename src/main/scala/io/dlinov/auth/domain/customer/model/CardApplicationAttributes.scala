package io.dlinov.auth.domain.customer.model

import java.util.UUID

import io.dlinov.auth.domain.customer.model.CustomerAttributes.{Address, NameAttribute}
import io.dlinov.auth.domain.customer.model.CustomerAttributes.{Address, NameAttribute}
import io.dlinov.auth.util.Implicits._

object CardApplicationAttributes {
  case class CardPin(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[\d]{6}"""))
  }

  case class CardApplicationType(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""))
  }

  case class CardType(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""))
  }

  case class NewCardApplicationDetails(
      nameOnCard: NameAttribute,
      cardPin: CardPin,
      deliveryAddress: Address
  )

  case class CardApplicationStatus(underlying: String) {
    assert(underlying.hasSomething)
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""))
  }

  case class CardApplication(
      userId: UUID,
      operationType: CardApplicationType,
      cardType: CardType,
      nameOnCard: NameAttribute,
      cardPin: CardPin,
      deliveryAddress: Address,
      status: CardApplicationStatus
  )
}
