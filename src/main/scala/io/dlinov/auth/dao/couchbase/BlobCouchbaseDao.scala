package io.dlinov.auth.dao.couchbase

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import com.couchbase.client.java.CouchbaseCluster
import com.couchbase.client.java.document.ByteArrayDocument
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment
import io.dlinov.auth.dao.generic.BlobTmpFDao
import io.dlinov.auth.AppConfig.CouchbaseConfig
import io.dlinov.auth.dao.Dao.DaoResponse

import scala.concurrent.duration.FiniteDuration

class BlobCouchbaseDao(couchbaseConfig: CouchbaseConfig) extends BlobTmpFDao {
  override def saveBlob(bytes: Array[Byte], name: String, expiration: FiniteDuration): IO[DaoResponse[String]] = {
    (for {
      doc ← EitherT(IO {
        val expiry = Instant.now().plusNanos(expiration.toNanos).getEpochSecond
        ByteArrayDocument.create(name, expiry.toInt, bytes)
      }.attempt)
      saved ← EitherT(IO(bucket.insert(doc)).attempt)
    } yield saved.id())
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .saveBlob(..,$name): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
  }

  override def loadBlob(path: String, userName: String): IO[DaoResponse[Option[Array[Byte]]]] = {
    (for {
      exists ← EitherT(IO(bucket.exists(path)).attempt)
      bytes ← EitherT {
        if (exists) {
          IO {
            Option(bucket.get(path, classOf[ByteArrayDocument]).content())
          }.attempt
        } else {
          IO.pure(Right(Option.empty[Array[Byte]]))
        }
      }
    } yield bytes)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .loadBlob($path,..): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
  }

  def removeBlob(name: String): IO[DaoResponse[Unit]] = {
    (for {
      exists ← EitherT(IO(bucket.exists(name)).attempt)
      result ← EitherT {
        if (exists) {
          IO {
            bucket.remove(name)
            ()
          }.attempt
        } else {
          IO.pure(Right(()))
        }
      }
    } yield result)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .saveBlob(..,$name): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
  }

  private val url = couchbaseConfig.url
  private val user = couchbaseConfig.user
  private val password = couchbaseConfig.password
  private val timeout = couchbaseConfig.timeout
  private val bucketName = couchbaseConfig.bucketName
  private val env = DefaultCouchbaseEnvironment.builder()
    .maxRequestLifetime(timeout * 2)
    .connectTimeout(timeout)
    .queryTimeout(timeout)
    .searchTimeout(timeout)
    .viewTimeout(timeout)
    .bootstrapHttpDirectPort(url.substring(url.lastIndexOf(':') + 1).toInt)
    .build()

  private lazy val cluster = {
    CouchbaseCluster
      .fromConnectionString(env, url)
      .authenticate(user, password)
  }
  private lazy val bucket = cluster.openBucket(bucketName)
}
