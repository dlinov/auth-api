package io.dlinov.auth

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import AppConfig._

import scala.concurrent.duration.{Duration, FiniteDuration}
import pureconfig.ConfigSource

case class AppConfig(
    db: DbConfig,
    logging: LogConfig,
    auth: AuthConfig,
    app: GlobalConfig,
    email: EmailConfig,
    proxy: ProxyConfig,
    server: ServerConfig
)

object AppConfig {
  def load: AppConfig = ConfigSource.default.at("fp").loadOrThrow[AppConfig]

  case class GlobalConfig(host: String)
  implicit val globalConfigReader: ConfigReader[GlobalConfig] = deriveReader[GlobalConfig]

  final case class DbConfig(
      url: String,
      user: String,
      password: String,
      minIdle: Int,
      poolSize: Int
  )
  implicit val dbConfigReader: ConfigReader[DbConfig] = deriveReader[DbConfig]

  final case class LogConfig(isEnabled: Boolean, logHeaders: Boolean, logBody: Boolean)
  implicit val logConfigReader: ConfigReader[LogConfig] = deriveReader[LogConfig]

  final case class PasswordConfig(
      default: String,
      nDigits: Int,
      nLowercase: Int,
      nUppercase: Int,
      nSpecialChars: Int,
      length: Int,
      duplicateCharsAllowed: Boolean
  )
  implicit val passwordConfigReader: ConfigReader[PasswordConfig] = deriveReader[PasswordConfig]

  final case class AuthConfig(
      tokenExpirationOffsetMinutes: Int,
      passwordGeneration: PasswordConfig,
      accountLockTimeout: Duration,
      maxBadLoginAttempts: Long,
      requireCaptcha: Boolean,
      recaptchaSecret: String,
      recaptchaUrl: String,
      secret: String
  )
  implicit val authConfigReader: ConfigReader[AuthConfig] = deriveReader[AuthConfig]

  final case class EmailConfig(
      host: String,
      port: Int,
      senderAddress: String,
      senderName: String,
      retryTimeout: FiniteDuration,
      maxRetries: Int
  )
  implicit val emailConfigReader: ConfigReader[EmailConfig] = deriveReader[EmailConfig]

  final case class ProxyConfig(host: String, port: Int) {
    override def toString() = s"$host:$port"
  }
  implicit val proxyConfigReader: ConfigReader[ProxyConfig] = deriveReader[ProxyConfig]

  /** Conservative server-hardening defaults (all overridable via env vars, see `application.conf`):
    * bound how long a connection may sit idle, how long the server will wait to receive request
    * headers, an overall per-request wall-clock timeout, and a maximum request body size, so a
    * slow/malicious/misbehaving client cannot exhaust server threads, connections or memory.
    */
  final case class ServerConfig(
      idleTimeout: FiniteDuration,
      headerTimeout: FiniteDuration,
      requestTimeout: FiniteDuration,
      maxRequestBodyBytes: Long
  )
  implicit val serverConfigReader: ConfigReader[ServerConfig] = deriveReader[ServerConfig]

  implicit val appConfigReader: ConfigReader[AppConfig] = deriveReader[AppConfig]
}
