package io.dlinov.auth.routes.dto

import java.time.ZonedDateTime
import java.util.UUID

import io.circe.Json

case class BackOfficeUserToRead(
    id: UUID,
    userName: String,
    email: String,
    phoneNumber: Option[String],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    lastLoginTimestamp: Option[Long],
    customData: Option[Json],
    role: RoleToRead,
    businessUnit: BusinessUnitToRead,
    permissions: Seq[PermissionToRead],
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime
)
