package io.dlinov.auth.routes.dto

import java.util.UUID

import io.dlinov.auth.domain.ErrorCode
import io.dlinov.auth.domain.ErrorCode
import io.dlinov.auth.routes.dto.ApiError.ErrorParams

final case class ApiError(id: UUID, code: ErrorCode, msg: String, params: Option[ErrorParams])

object ApiError {
  final case class ErrorParams(fieldName: String, fieldValue: String)
}
