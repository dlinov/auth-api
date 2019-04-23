package io.dlinov.auth.dao

import cats.effect.ContextShift
import doobie.util.transactor.Transactor

trait DBFApi[F[_]] {
  implicit def cs: ContextShift[F]

  def transactor: F[Transactor[F]]

}
