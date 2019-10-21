package io.dlinov.auth.domain.auth.entities

import java.time.ZonedDateTime

import io.dlinov.auth.dao.Dao.EntityId

case class Role(
    id: EntityId,
    name: String,
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime
)
