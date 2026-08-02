package io.dlinov.auth.util

import cats.effect.Sync
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}

import scala.concurrent.duration.Duration

/** A minimal Caffeine-backed cache with per-entry TTL.
  *
  * scalacache (previously used here) has no stable Scala 3 / cats-effect 3 release (only milestone
  * builds exist as of this migration), so this small, dependency-light wrapper replaces it while
  * preserving the same get/put(with ttl)/remove semantics.
  */
final class SimpleExpiringCache[F[_], V](implicit F: Sync[F]) {
  final private case class Entry(value: V, expiresAtNanos: Option[Long])

  final private val underlying: Cache[String, Entry] = Caffeine.newBuilder().build[String, Entry]()

  def get(key: String): F[Option[V]] = F.delay {
    Option(underlying.getIfPresent(key)) match {
      case Some(Entry(value, Some(expiresAtNanos))) if System.nanoTime() >= expiresAtNanos =>
        underlying.invalidate(key)
        None
      case Some(Entry(value, _)) => Some(value)
      case None                  => None
    }
  }

  def put(key: String, value: V, ttl: Option[Duration]): F[Unit] = F.delay {
    val expiresAtNanos = ttl.filter(_.isFinite).map(d => System.nanoTime() + d.toNanos)
    underlying.put(key, Entry(value, expiresAtNanos))
  }

  def remove(key: String): F[Unit] = F.delay {
    underlying.invalidate(key)
  }
}
