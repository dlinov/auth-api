package io.dlinov.auth.dao

import cats.Monad
import cats.effect.ContextShift
import doobie.util.transactor.Transactor

abstract class DBFApi[F[_]: Monad] {
  implicit def cs: ContextShift[F]

  def transactor: F[Transactor[F]]

}
