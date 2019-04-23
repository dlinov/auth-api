package io.dlinov.auth.util

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

object Constants {
  val EmptyUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

  val MinDateTime: ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC)
}
