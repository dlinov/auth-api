package io.dlinov.auth

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.syntax.functor._
import monix.eval.{Task, TaskApp}
import org.http4s.server.blaze._

object MonixBoot extends TaskApp with Bootable[Task] {

  implicit override protected def contextShift: ContextShift[Task] = Task.contextShift
  implicit override protected def timer: Timer[Task]               = Task.timer
  implicit override protected def ce: ConcurrentEffect[Task]       = catsEffect

  def run(args: List[String]): Task[ExitCode] = {
    BlazeServerBuilder[Task]
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(services)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
