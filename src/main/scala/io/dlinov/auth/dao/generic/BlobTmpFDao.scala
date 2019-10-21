package io.dlinov.auth.dao.generic

import io.dlinov.auth.dao.Dao
import io.dlinov.auth.dao.Dao.DaoResponse

import scala.concurrent.duration._

trait BlobTmpFDao[F[_]] extends Dao {

  def defaultDuration: FiniteDuration = 30.days

  def saveBlob(bytes: Array[Byte], name: String, expiration: FiniteDuration): F[DaoResponse[String]]

  def loadBlob(path: String, userName: String): F[DaoResponse[Option[Array[Byte]]]]

  def removeBlob(name: String): F[DaoResponse[Unit]]

}
