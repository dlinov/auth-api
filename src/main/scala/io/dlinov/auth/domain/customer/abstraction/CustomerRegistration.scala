package io.dlinov.auth.domain.customer.abstraction

import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.domain.customer.model.Account
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.customer.model.BusinessUsers.RegisteredButNotActivatedBusinessUser
import io.dlinov.auth.domain.customer.model.CardApplicationAttributes.{CardApplication, CardPin, NewCardApplicationDetails}
import io.dlinov.auth.domain.customer.model.CustomerAttributes._
import io.dlinov.auth.domain.customer.model._

import scala.concurrent.Future

trait CustomerRegistration {

  def registerBusinessUser(
    username: LoginUsername, msisdn: Option[Msisdn], company: NameAttribute,
    tier: Option[CustomerTier], subscription: Option[CustomerSubscription],
    newCardApplicationDetails: Option[NewCardApplicationDetails])(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]]

  def createDefaultBusinessUserAccount(user: RegisteredButNotActivatedBusinessUser)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Account]]

  def createCardApplication(
    user: RegisteredButNotActivatedBusinessUser,
    cardPin: Option[CardPin], nameOnCard: NameAttribute,
    cardDeliveryAddress: Address)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[CardApplication]]

}
