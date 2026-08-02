package io.dlinov.auth

import cats.effect._
import com.comcast.ip4s.{Ipv4Address, Port}
import com.zaxxer.hikari.HikariDataSource
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{EntityLimiter, Timeout}
import io.dlinov.auth.dao.hikari.HikariDBFApi
import io.dlinov.auth.dao.hikari.ec.ConnectECWrapper

object CatsIOBoot extends IOApp {

  /** The three resources whose lifecycle used to be mismanaged (self-constructed `lazy val`s in
    * `Bootable`, either never closed at all - the Hikari datasource and its connect executor - or
    * rebuilt from scratch on every single proxied/captcha request - the Ember http client):
    * acquired exactly once here, for the whole application's lifetime, and released, in reverse
    * order, on shutdown.
    */
  private def resources(
      dbConfig: AppConfig.DbConfig
  ): Resource[IO, (HikariDataSource, ConnectECWrapper, Client[IO])] = {
    for {
      dataSource <- HikariDBFApi.dataSourceResource[IO](dbConfig)
      connectEC  <- ConnectECWrapper.resource[IO](dbConfig.poolSize)
      httpClient <- EmberClientBuilder.default[IO].build
    } yield (dataSource, connectEC, httpClient)
  }

  def run(args: List[String]): IO[ExitCode] = {
    val cfg = AppConfig.load
    resources(cfg.db)
      .use { case (ds, ec, client) =>
        val bootable: Bootable[IO] = new Bootable[IO] {
          implicit override protected def asyncF: Async[IO]         = IO.asyncForIO
          override lazy val appConfig: AppConfig                    = cfg
          override protected def hikariDataSource: HikariDataSource = ds
          override protected def connectEC: ConnectECWrapper        = ec
          override protected def httpClient: Client[IO]             = client
        }
        val serverConfig   = bootable.serverConfig
        val boundedHttpApp =
          EntityLimiter.httpApp(bootable.services, serverConfig.maxRequestBodyBytes)
        val httpApp = Timeout(serverConfig.requestTimeout)(boundedHttpApp)
        EmberServerBuilder
          .default[IO]
          .withHost(Ipv4Address.Wildcard)
          .withPort(Port.fromInt(9000).get)
          .withIdleTimeout(serverConfig.idleTimeout)
          .withRequestHeaderReceiveTimeout(serverConfig.headerTimeout)
          .withHttpApp(httpApp)
          .build
          .useForever
      }
      .as(ExitCode.Success)
  }
}
