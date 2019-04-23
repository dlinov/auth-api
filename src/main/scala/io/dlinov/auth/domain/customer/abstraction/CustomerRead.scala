package io.dlinov.auth.domain.customer.abstraction

import java.time.LocalDate
import java.util.UUID

import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.customer.model.BusinessUsers._
import io.dlinov.auth.domain.customer.model.CustomerAttributes._

import scala.concurrent.Future

trait CustomerRead {
  def getActivatedBusinessUserById(id: UUID): Future[ServiceResponse[ActivatedBusinessUser]]

  def getWaitingForActivationBusinessUserById(id: UUID): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]]

  def getActivatedBusinessUserByName(username: LoginUsername): Future[ServiceResponse[ActivatedBusinessUser]]

  def getWaitingForActivationBusinessUserByName(username: LoginUsername): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]]

  def findWaitingForActivationBusinessUserByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[RegisteredButNotActivatedBusinessUser]]]

  def countWaitingForActivationBusinessUserByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute]): Future[ServiceResponse[Int]]

  def findActivatedBusinessUserByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute],
    tier: Option[CustomerTier], segment: Option[CustomerSegment], subscription: Option[CustomerSubscription],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[RegisteredButNotActivatedBusinessUser]]]

  def countActivatedBusinessUserByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    tier: Option[CustomerTier], segment: Option[CustomerSegment], subscription: Option[CustomerSubscription],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute]): Future[ServiceResponse[Int]]

}
