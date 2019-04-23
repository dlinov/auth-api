package io.dlinov.auth.dao.hdfs

import cats.data.EitherT
import cats.effect.IO
import io.dlinov.auth.dao.generic.BlobFDao
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import io.dlinov.auth.AppConfig.HdfsConfig
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.dao.generic.BlobFDao

class BlobHdfsDao(config: HdfsConfig) extends BlobFDao {

  override def saveBlob(bytes: Array[Byte], name: String): IO[DaoResponse[String]] = {
    (for {
      hdfsFileSystem ← EitherT(IO(FileSystem.get(hdfsConfiguration)).attempt)
      path ← EitherT(IO(new Path(name)).attempt)
      hdfsOutputFile ← EitherT(IO(hdfsFileSystem.create(path)).attempt)
      _ ← EitherT(IO {
        hdfsOutputFile.write(bytes)
        hdfsOutputFile.close()
        hdfsFileSystem.close()
      }.attempt)
    } yield name)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .saveBlob(..,$name): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
  }

  override def loadBlob(path: String, userName: String): IO[DaoResponse[Option[Array[Byte]]]] = {
    (for {
      hdfsFileSystem ← EitherT(IO(FileSystem.get(hdfsConfiguration)).attempt)
      path ← EitherT(IO(new Path(path)).attempt)
      exists ← EitherT(IO(hdfsFileSystem.exists(path)).attempt)
      fileContent ← EitherT {
        if (exists) {
          IO {
            val hdfsFileStream = hdfsFileSystem.open(path)
            val length = hdfsFileStream.available()
            val bytes = new Array[Byte](length)
            hdfsFileStream.readFully(bytes)
            hdfsFileStream.close()
            Some(bytes)
          }.attempt
        } else {
          IO.pure(Right(Option.empty[Array[Byte]]))
        }
      }
      _ ← EitherT(IO(hdfsFileSystem.close()).attempt)
    } yield fileContent)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .loadBlob($path,$userName): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
  }

  private val hdfsConfiguration: Configuration = new Configuration()
  hdfsConfiguration.set("fs.defaultFS", config.uri)
  hdfsConfiguration.set("fs.hdfs.impl", classOf[org.apache.hadoop.hdfs.DistributedFileSystem].getName)
  hdfsConfiguration.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)
  hdfsConfiguration.set("dfs.support.append", config.dfsSupportAppend)
  hdfsConfiguration.set("dfs.replication", config.dfsReplication)
}
