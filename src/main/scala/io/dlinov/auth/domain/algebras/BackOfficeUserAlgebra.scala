package io.dlinov.auth.domain.algebras

import java.util.UUID

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.dao.generic.BackOfficeUserFDao
import io.dlinov.auth.domain.{BaseService, PaginatedResult}
import io.dlinov.auth.domain.algebras.services.NotificationService
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Email, Notifications}
import io.dlinov.auth.domain.{BaseService, PaginatedResult}
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Email, Notifications}
import io.dlinov.auth.domain.algebras.services.NotificationService
import io.dlinov.auth.dao.generic.BackOfficeUserFDao

class BackOfficeUserAlgebra(
    dao: BackOfficeUserFDao,
    businessUnitAlgebra: BusinessUnitAlgebra,
    roleAlgebra: RoleAlgebra,
    passwordAlgebra: PasswordAlgebra,
    notificationService: NotificationService) extends BaseService {
  import BaseService._

  def create(
    userName: String,
    email: Email,
    phoneNumber: Option[String],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[String],
    roleId: UUID,
    businessUnitId: UUID,
    createdBy: String,
    maybeReactivate: Option[Boolean]): IO[ServiceResponse[BackOfficeUser]] = {
    (for {
      _ ← EitherT(roleAlgebra.findById(roleId))
      _ ← EitherT(businessUnitAlgebra.findById(businessUnitId))
      generatedPassword = passwordAlgebra.generatePassword
      passwordHash = passwordAlgebra.hashPassword(generatedPassword)
      user ← EitherT(dao.create(
        userName = userName,
        password = passwordHash,
        email = email,
        phoneNumber = phoneNumber,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        description = description,
        homePage = homePage,
        activeLanguage = activeLanguage,
        customData = customData,
        roleId = roleId,
        businessUnitId = businessUnitId,
        createdBy = createdBy,
        reactivate = maybeReactivate.getOrElse(false))
        .map(_.asServiceResponse))
      // TODO: send notification in async way
      _ ← EitherT(emailPassword(generatedPassword, user))
    } yield user).value
  }

  def findById(id: UUID): IO[ServiceResponse[BackOfficeUser]] = {
    for {
      maybeUserOrError ← dao.findById(id)
    } yield maybeUserOrError
      .asServiceResponse
      .flatMap(_.toRight(notFoundEntityError(s"User with id '$id' was not found")))
  }

  def findByName(name: String): IO[ServiceResponse[BackOfficeUser]] = {
    for {
      userOrError ← dao.findByName(name)
    } yield userOrError
      .asServiceResponse
      .flatMap(_.toRight(notFoundEntityError(s"User with name '$name' was not found")))
  }

  def findAll(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int],
    maybeFirstName: Option[String],
    maybeLastName: Option[String],
    maybeEmail: Option[String],
    maybePhoneNumber: Option[String]): IO[ServiceResponse[PaginatedResult[BackOfficeUser]]] = {
    for {
      usersOrError ← dao.findAll(
        maybeLimit = maybeLimit,
        maybeOffset = maybeOffset,
        maybeFirstName = maybeFirstName,
        maybeLastName = maybeLastName,
        maybeEmail = maybeEmail,
        maybePhoneNumber = maybePhoneNumber)
    } yield usersOrError.asServiceResponse
  }

  def update(
    id: UUID,
    email: Option[Email],
    phoneNumber: Option[String],
    firstName: Option[String],
    middleName: Option[String],
    lastName: Option[String],
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[String],
    roleId: Option[UUID],
    businessUnitId: Option[UUID],
    updatedBy: String): IO[ServiceResponse[BackOfficeUser]] = {
    for {
      updatedUser ← dao.update(
        id = id,
        email = email.map(_.value),
        phoneNumber = phoneNumber,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        description = description,
        homePage = homePage,
        activeLanguage = activeLanguage,
        customData = customData,
        roleId = roleId,
        businessUnitId = businessUnitId,
        updatedBy = updatedBy)
    } yield updatedUser
      .asServiceResponse
      .flatMap(_.toRight(notFoundEntityError(s"User with id '$id' was not found")))
  }

  def remove(id: UUID, updatedBy: String): IO[ServiceResponse[UUID]] = {
    for {
      maybeUser ← dao.remove(id, updatedBy)
    } yield maybeUser
      .asServiceResponse
      .flatMap(_.map(_.id).toRight(notFoundEntityError(s"User with id '$id' was not found")))
  }

  def login(name: String, passwordHash: String): IO[ServiceResponse[BackOfficeUser]] = {
    for {
      userOrError ← dao.login(name, passwordHash)
    } yield userOrError.toRight(notAuthorizedError(s"Wrong credentials for user $name"))
  }

  def updatePassword(
    name: String,
    oldPasswordHash: String,
    passwordHash: String): IO[ServiceResponse[BackOfficeUser]] = {
    for {
      maybeUser ← dao.updatePassword(name, oldPasswordHash, passwordHash)
    } yield maybeUser.toRight(notAuthorizedError(s"Failed to update password for user $name. Check credentials"))
  }

  def resetPassword(
    name: String,
    passwordHash: String): IO[ServiceResponse[BackOfficeUser]] = {
    for {
      maybeUser ← dao.resetPassword(name, passwordHash)
    } yield maybeUser.toRight(notAuthorizedError(s"Failed to reset password for user '$name'. Check if user exists"))
  }

  private def emailPassword(generatedPassword: String, user: BackOfficeUser): IO[ServiceResponse[Unit]] = {
    val topic = "Backoffice credentials"
    val recipientName = s"${user.firstName} ${user.lastName}"
    val message =
      s"""Hello $recipientName,
         |
         |Your registration was successful.
         |Username: ${user.userName}
         |Password: $generatedPassword
         |
         |Kind regards,
         |BackOffice API""".stripMargin
    notificationService.sendNotification(
      Notifications.textNotification(user, topic, message))
  }

}
