package io.dlinov.auth.domain.auth.entities

import org.cvogt.scala.EnumerateSingletons

sealed trait Status {
  val value: Int
}

object Status {
  case object Inactive extends Status {
    override final val value: Int = 0
  }

  case object Active extends Status {
    override final val value: Int = 1
  }

  lazy val all: Set[Status] = EnumerateSingletons[Status]
  lazy val allMap: Map[Int, Status] = all.map(s ⇒ s.value → s).toMap

  def from: Int ⇒ Status = allMap
}
