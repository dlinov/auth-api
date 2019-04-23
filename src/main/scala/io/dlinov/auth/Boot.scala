package io.dlinov.auth

import cats.effect._
import cats.implicits._
import org.http4s.server.blaze._

object Boot extends IOApp with Bootable {
  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(services)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}

