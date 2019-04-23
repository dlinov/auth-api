package io.dlinov.auth

import pureconfig.generic.auto._ // important!
import AppConfig._

import scala.concurrent.duration.{Duration, FiniteDuration}

case class AppConfig(
    db: DbConfig,
    logging: LogConfig,
    auth: AuthConfig,
    app: GlobalConfig,
    email: EmailConfig,
    couchbase: CouchbaseConfig,
    hdfs: HdfsConfig,
    proxy: ProxyConfig)

object AppConfig {
  def load: AppConfig = pureconfig.loadConfigOrThrow[AppConfig]("fp")

  case class GlobalConfig(
      host: String)

  final case class DbConfig(
      url: String,
      user: String,
      password: String,
      minIdle: Int,
      poolSize: Int)

  final case class LogConfig(
      isEnabled: Boolean,
      logHeaders: Boolean,
      logBody: Boolean)

  final case class AuthConfig(
      tokenExpirationOffsetMinutes: Int,
      passwordGeneration: PasswordConfig,
      accountLockTimeout: Duration,
      maxBadLoginAttempts: Long,
      requireCaptcha: Boolean,
      recaptchaSecret: String,
      recaptchaUrl: String,
      secret: String)

  final case class PasswordConfig(
      default: String,
      nDigits: Int,
      nLowercase: Int,
      nUppercase: Int,
      nSpecialChars: Int,
      length: Int,
      duplicateCharsAllowed: Boolean)

  final case class EmailConfig(
      host: String,
      port: Int,
      senderAddress: String,
      senderName: String,
      retryTimeout: FiniteDuration,
      maxRetries: Int)

  final case class CouchbaseConfig(
      url: String,
      user: String,
      password: String,
      timeout: Long,
      bucketName: String)

  final case class HdfsConfig(
      uri: String,
      dfsReplication: String,
      dfsSupportAppend: String)

  final case class ProxyConfig(
      host: String,
      port: Int) {
    override def toString() = s"$host:$port"
  }
}
