package io.dlinov.auth.routes.dto

import java.util.UUID

import io.circe.Json
import io.dlinov.auth.domain.auth.entities.Email

case class BackOfficeUserToUpdate(
    email: Option[Email],
    phoneNumber: Option[String],
    firstName: Option[String],
    middleName: Option[String],
    lastName: Option[String],
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[Json],
    roleId: Option[UUID],
    businessUnitId: Option[UUID])

object BackOfficeUserToUpdate {
  val empty = BackOfficeUserToUpdate(
    email = None,
    phoneNumber = None,
    firstName = None,
    middleName = None,
    lastName = None,
    description = None,
    homePage = None,
    activeLanguage = None,
    customData = None,
    roleId = None,
    businessUnitId = None)
}
