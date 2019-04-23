package io.dlinov.auth.dao.generic

import cats.effect.IO
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.dao.Dao.DaoResponse

import scala.concurrent.duration._

trait BlobTmpFDao extends Dao {

  def defaultDuration: FiniteDuration = 30.days

  def saveBlob(bytes: Array[Byte], name: String, expiration: FiniteDuration): IO[DaoResponse[String]]

  def loadBlob(path: String, userName: String): IO[DaoResponse[Option[Array[Byte]]]]

  def removeBlob(name: String): IO[DaoResponse[Unit]]

}
