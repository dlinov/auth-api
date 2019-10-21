package io.dlinov.auth.domain

import java.util.UUID

import cats.syntax.either._
import io.dlinov.auth.util.Logging
import io.dlinov.auth.dao.Dao.DaoResponse

trait BaseService extends Logging {

  protected def unknownError(msg: String, id: UUID = UUID.randomUUID()): ServiceError =
    ServiceError.unknownError(id, msg)

  protected def duplicateEntityError(
      msg: String,
      field: String,
      value: String,
      id: UUID = UUID.randomUUID()
  ): ServiceError =
    ServiceError.duplicateEntityError(id, msg, field, value)

  protected def notFoundEntityError(msg: String, id: UUID = UUID.randomUUID()): ServiceError =
    ServiceError.notFoundEntityError(id, msg)

  protected def validationError(msg: String, id: UUID = UUID.randomUUID()): ServiceError =
    ServiceError.validationError(id, msg)

  protected def notAuthorizedError(msg: String, id: UUID = UUID.randomUUID()): ServiceError =
    ServiceError.notAuthorizedError(id, msg)

}

object BaseService {

  type ServiceResponse[T] = Either[ServiceError, T]

  implicit class DaoToServiceResponseConverter[T](val daoResponse: DaoResponse[T]) extends AnyVal {
    def asServiceResponse: ServiceResponse[T] = daoResponse.leftMap(ServiceError.fromDaoError)
  }
}
