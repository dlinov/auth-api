package io.dlinov.auth.dao

import doobie.util.transactor.Transactor

abstract class DBFApi[F[_]] {
  def transactor: F[Transactor[F]]

}
