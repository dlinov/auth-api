package io.dlinov.auth.domain.auth.entities

sealed trait Status {
  val value: Int
}

object Status {
  case object Inactive extends Status {
    final override val value: Int = 0
  }

  case object Active extends Status {
    final override val value: Int = 1
  }

  // org.cvogt.scala-extensions (EnumerateSingletons macro) is abandoned and has no Scala 3
  // support; the sealed trait only ever had these two case objects, so list them explicitly.
  lazy val all: Set[Status]         = Set(Inactive, Active)
  lazy val allMap: Map[Int, Status] = all.map(s => s.value -> s).toMap

  def from: Int => Status = allMap
}
