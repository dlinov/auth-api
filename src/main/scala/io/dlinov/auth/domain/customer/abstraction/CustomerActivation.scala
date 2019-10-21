package io.dlinov.auth.domain.customer.abstraction

import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.domain.customer.model.BusinessUsers.RegisteredButNotActivatedBusinessUser
import io.dlinov.auth.domain.BaseService.ServiceResponse
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.domain.customer.model.BusinessUsers.RegisteredButNotActivatedBusinessUser
import io.dlinov.auth.domain.customer.model.CustomerAttributes.{
  ActivationRequirement,
  CustomerStatus,
  LoginUsername
}

import scala.concurrent.Future

trait CustomerActivation {

  def activateBusinessUserOnTheFly(
      user: RegisteredButNotActivatedBusinessUser,
      requiredDocuments: Seq[ActivationRequirement]
  )(implicit doneByBackOfficeUser: BackOfficeUser): Future[ServiceResponse[CustomerStatus]]

  def activateBusinessUser(user: RegisteredButNotActivatedBusinessUser)(
      implicit doneByBackOfficeUser: BackOfficeUser
  ): Future[ServiceResponse[CustomerStatus]]

  def notifyBusinessUserForActivation(username: LoginUsername): Future[ServiceResponse[Unit]]
}
