package io.dlinov.auth

import cats.effect._
import cats.syntax.functor._
import org.http4s.server.blaze._

object CatsIOBoot extends IOApp with Bootable[IO] {
  implicit override protected def ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(services)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
