package io.dlinov.auth.domain.auth.entities

import java.time.ZonedDateTime
import java.util.UUID

case class BackOfficeUser(
    id: UUID,
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
    lastLoginTimestamp: Option[Long],
    role: Role,
    businessUnit: BusinessUnit,
    permissions: Seq[Permission],
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime
)
