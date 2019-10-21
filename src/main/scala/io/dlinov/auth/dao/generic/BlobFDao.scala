package io.dlinov.auth.dao.generic

import io.dlinov.auth.dao.Dao
import io.dlinov.auth.dao.Dao.DaoResponse

trait BlobFDao[F[_]] extends Dao {
  def saveBlob(bytes: Array[Byte], name: String): F[DaoResponse[String]]

  // or id
  def loadBlob(path: String, userName: String): F[DaoResponse[Option[Array[Byte]]]]
}
