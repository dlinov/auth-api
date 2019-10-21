package io.dlinov.auth.dao.hdfs

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.functor._
import io.dlinov.auth.dao.generic.BlobFDao
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import io.dlinov.auth.AppConfig.HdfsConfig
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.dao.generic.BlobFDao

class BlobHdfsDao[F[_]](config: HdfsConfig)(implicit syncF: Sync[F]) extends BlobFDao[F] {

  private def attemptT[T](thunk: ⇒ T): EitherT[F, Throwable, T] = syncF.attemptT(syncF.delay(thunk))

  override def saveBlob(bytes: Array[Byte], name: String): F[DaoResponse[String]] = {
    (for {
      hdfsFileSystem ← attemptT(FileSystem.get(hdfsConfiguration))
      path           ← attemptT(new Path(name))
      hdfsOutputFile ← attemptT(hdfsFileSystem.create(path))
      _ ← attemptT({
        hdfsOutputFile.write(bytes)
        hdfsOutputFile.close()
        hdfsFileSystem.close()
      })
    } yield name)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .saveBlob(..,$name): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
      .widen[DaoResponse[String]]
  }

  override def loadBlob(path: String, userName: String): F[DaoResponse[Option[Array[Byte]]]] = {
    (for {
      hdfsFileSystem ← attemptT(FileSystem.get(hdfsConfiguration))
      path           ← attemptT(new Path(path))
      exists         ← attemptT(hdfsFileSystem.exists(path))
      fileContent ← if (exists) {
        attemptT[Option[Array[Byte]]]({
          val hdfsFileStream = hdfsFileSystem.open(path)
          val length         = hdfsFileStream.available()
          val bytes          = new Array[Byte](length)
          hdfsFileStream.readFully(bytes)
          hdfsFileStream.close()
          Option(bytes)
        })
      } else {
        EitherT.fromEither[F](Right(Option.empty[Array[Byte]]))
      }
      _ ← attemptT(hdfsFileSystem.close())
    } yield fileContent)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .loadBlob($path,$userName): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
      .widen[DaoResponse[Option[Array[Byte]]]]
  }

  private val hdfsConfiguration: Configuration = new Configuration()
  hdfsConfiguration.set("fs.defaultFS", config.uri)
  hdfsConfiguration.set(
    "fs.hdfs.impl",
    classOf[org.apache.hadoop.hdfs.DistributedFileSystem].getName
  )
  hdfsConfiguration.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)
  hdfsConfiguration.set("dfs.support.append", config.dfsSupportAppend)
  hdfsConfiguration.set("dfs.replication", config.dfsReplication)
}
