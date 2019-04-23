package io.dlinov.auth.routes.dto

import java.util.UUID

import io.circe.Json
import io.dlinov.auth.domain.auth.entities.Email

case class BackOfficeUserToCreate(
    userName: String,
    roleId: UUID,
    businessUnitId: UUID,
    email: Email,
    phoneNumber: Option[String],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[Json])
