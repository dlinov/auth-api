package io.dlinov.auth.domain.customer.abstraction

import java.util.UUID

import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Email}
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.domain.customer.model.CustomerAttributes._

import scala.concurrent.Future

trait CustomerUpdate {

  def updateCompanyName(buIdToFind: UUID, newCompanyName: NameAttribute)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateSegment(buIdToFind: UUID, newSegment: CustomerSegment)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateTier(buIdToFind: UUID, newTier: CustomerTier)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateSubscription(buIdToFind: UUID, newSubscription: CustomerSubscription)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateEmails(buIdToFind: UUID, newEmails: Set[Email])(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateStatus(buIdToFind: UUID, newStatus: CustomerStatus)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateMsisdn(buIdToFind: UUID, newMsisdn: Msisdn)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateBusinessUserType(buIdToFind: UUID, newBuType: BusinessUserType)(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updateAddresses(buIdToFind: UUID, newAddresses: Set[Address])(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

  def updatePhoneNumbers(buIdToFind: UUID, newPhoneNumbers: Set[PhoneNumber])(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[Unit]]

}
