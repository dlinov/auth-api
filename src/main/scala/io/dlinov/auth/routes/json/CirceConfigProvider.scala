package io.dlinov.auth.routes.json

import io.circe.derivation.Configuration

trait CirceConfigProvider {
  implicit val config: Configuration
}

object CirceConfigProvider {
  val snakeConfig: Configuration =
    Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames.withDefaults
}
