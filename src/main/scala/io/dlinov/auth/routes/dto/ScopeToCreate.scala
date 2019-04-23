package io.dlinov.auth.routes.dto

import java.util.UUID

case class ScopeToCreate(
    name: String,
    parentId: Option[UUID],
    description: Option[String] = None)
