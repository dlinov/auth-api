package io.dlinov.auth.dao.hikari.ec

import scala.concurrent.ExecutionContext

class ConnectECWrapper(val ec: ExecutionContext) extends AnyVal

object ConnectECWrapper {
  import java.util.concurrent.{Executors, ThreadFactory}

  import cats.effect.{Resource, Sync}

  /** A bounded (rather than unbounded `Executors.newCachedThreadPool()`) executor used only for
    * blocking JDBC connection acquisition, sized to the Hikari connection pool itself: there is
    * never a point in having more threads blocked waiting on a connection than there are
    * connections to hand out, and an unbounded cached pool can otherwise grow without limit under
    * load. Released via `Resource` so the pool is shut down on application/test suite shutdown
    * rather than leaking non-daemon threads.
    */
  def resource[F[_]: Sync](poolSize: Int): Resource[F, ConnectECWrapper] = {
    val threadFactory: ThreadFactory = new ThreadFactory {
      private val delegate                        = Executors.defaultThreadFactory()
      override def newThread(r: Runnable): Thread = {
        val t = delegate.newThread(r)
        t.setName(s"db-connect-ec-${t.threadId()}")
        t.setDaemon(true)
        t
      }
    }
    Resource
      .make(Sync[F].delay(Executors.newFixedThreadPool(math.max(1, poolSize), threadFactory)))(es =>
        Sync[F].delay(es.shutdown())
      )
      .map(es => new ConnectECWrapper(ExecutionContext.fromExecutor(es)))
  }
}
