package io.dlinov.auth.dao.couchbase

import java.time.Instant

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import com.couchbase.client.java.CouchbaseCluster
import com.couchbase.client.java.document.ByteArrayDocument
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment
import io.dlinov.auth.dao.generic.BlobTmpFDao
import io.dlinov.auth.AppConfig.CouchbaseConfig
import io.dlinov.auth.dao.Dao.DaoResponse

import scala.concurrent.duration.FiniteDuration

class BlobCouchbaseDao[F[_]](couchbaseConfig: CouchbaseConfig)(
    implicit syncF: Sync[F]
) extends BlobTmpFDao[F] {

  private def attemptT[T](f: ⇒ T): EitherT[F, Throwable, T] =
    syncF.attemptT(syncF.delay(f))

  override def saveBlob(
      bytes: Array[Byte],
      name: String,
      expiration: FiniteDuration
  ): F[DaoResponse[String]] = {
    (for {
      doc ← attemptT {
        val expiry = Instant.now().plusNanos(expiration.toNanos).getEpochSecond
        ByteArrayDocument.create(name, expiry.toInt, bytes)
      }
      saved ← attemptT(bucket.insert(doc))
    } yield saved.id())
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .saveBlob(..,$name): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
      .widen
  }

  override def loadBlob(path: String, userName: String): F[DaoResponse[Option[Array[Byte]]]] = {
    (for {
      exists ← attemptT(bucket.exists(path))
      bytes ← if (exists) {
        attemptT {
          Option(bucket.get(path, classOf[ByteArrayDocument]).content())
        }
      } else {
        // TODO: think if it's possible to make it easier
        EitherT.fromEither[F]((Option.empty[Array[Byte]]).asRight[Throwable])
      }
    } yield bytes)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .loadBlob($path,..): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
      .widen
  }

  def removeBlob(name: String): F[DaoResponse[Unit]] = {
    (for {
      exists ← attemptT(bucket.exists(name))
      result ← if (exists) {
        attemptT {
          bucket.remove(name)
          ()
        }
      } else {
        EitherT.fromEither[F](().asRight[Throwable])
      }
    } yield result)
      .leftMap { exc ⇒
        val msg = s"Unexpected error in .saveBlob(..,$name): " + exc.getMessage
        logger.warn(msg, exc)
        genericDbError(msg)
      }
      .value
      .widen
  }

  private val url        = couchbaseConfig.url
  private val user       = couchbaseConfig.user
  private val password   = couchbaseConfig.password
  private val timeout    = couchbaseConfig.timeout
  private val bucketName = couchbaseConfig.bucketName
  private val env = DefaultCouchbaseEnvironment
    .builder()
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
