package io.dlinov.auth.domain.auth.entities

import java.time.ZonedDateTime

import io.dlinov.auth.dao.Dao.EntityId

case class Scope(
    id: EntityId,
    parentId: Option[EntityId],
    name: String,
    description: Option[String] = None,
    createdBy: String,
    updatedBy: String,
    createdTime: ZonedDateTime,
    updatedTime: ZonedDateTime)
