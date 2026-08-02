package io.dlinov.auth.dao.hikari

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.dlinov.auth.AppConfig.DbConfig
import io.dlinov.auth.dao.hikari.ec.ConnectECWrapper
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Random

/** Proves the two resources that used to leak (a `HikariDataSource` built with `new
  * HikariDataSource(...)` and never closed, and an unbounded `Executors.newCachedThreadPool()`
  * never shut down) are now correctly released when their owning `Resource` is released - e.g. on
  * application shutdown, or in a test suite's `afterAll` (see `Http4sSpec`).
  */
class ResourceLifecycleSpec extends AnyWordSpecLike with Matchers {

  private def freshDbConfig(): DbConfig = {
    val suffix = Random.nextInt(Short.MaxValue)
    DbConfig(
      url = s"jdbc:h2:mem:resource_lifecycle_spec_$suffix;" +
        "MODE=MYSQL;DATABASE_TO_UPPER=FALSE;IGNORECASE=TRUE;INIT=RUNSCRIPT FROM 'classpath:init.sql'",
      user = "",
      password = "",
      minIdle = 1,
      poolSize = 3
    )
  }

  "HikariDBFApi.dataSourceResource" should {
    "close the underlying HikariDataSource on release" in {
      val ds = HikariDBFApi.dataSourceResource[IO](freshDbConfig()).allocated.unsafeRunSync()
      val (dataSource, release) = ds
      dataSource.isClosed mustBe false
      release.unsafeRunSync()
      dataSource.isClosed mustBe true
    }
  }

  "ConnectECWrapper.resource" should {
    "shut down its underlying executor on release" in {
      val (wrapper, release) = ConnectECWrapper.resource[IO](3).allocated.unsafeRunSync()
      // exercise the EC once to prove it is usable before release
      val ran = new java.util.concurrent.atomic.AtomicBoolean(false)
      wrapper.ec.execute(() => ran.set(true))
      Thread.sleep(100)
      ran.get() mustBe true

      release.unsafeRunSync()
      // a shut-down ExecutionContext (backed by a shut-down ExecutorService) rejects new tasks
      an[Throwable] must be thrownBy {
        wrapper.ec.execute(() => ())
      }
    }
  }
}
