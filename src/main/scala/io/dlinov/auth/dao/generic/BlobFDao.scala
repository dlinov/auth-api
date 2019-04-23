package io.dlinov.auth.dao.generic

import cats.effect.IO
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.dao.Dao.DaoResponse

trait BlobFDao extends Dao {
  def saveBlob(bytes: Array[Byte], name: String): IO[DaoResponse[String]]

  // or id
  def loadBlob(path: String, userName: String): IO[DaoResponse[Option[Array[Byte]]]]
}
