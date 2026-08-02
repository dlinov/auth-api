package io.dlinov.auth.domain.algebras

import java.util.Base64

import io.dlinov.auth.{AppConfig, PasswordBehavior}
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}

class PasswordAlgebra(config: AppConfig) extends PasswordBehavior {

  private lazy val authConfig        = config.auth
  private lazy val pwdGenConfig      = authConfig.passwordGeneration
  private lazy val secretKey: String =
    Base64.getEncoder.encodeToString(authConfig.secret.reverse.getBytes)
  private lazy val hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey)

  override protected val defaultPassword: String = authConfig.passwordGeneration.default

  override lazy val nDigits: Int             = pwdGenConfig.nDigits
  override lazy val nLowercase: Int          = pwdGenConfig.nLowercase
  override lazy val nUppercase: Int          = pwdGenConfig.nUppercase
  override lazy val nSpecial: Int            = pwdGenConfig.nSpecialChars
  override lazy val minPasswordLength: Int   = pwdGenConfig.length
  override val duplicateCharAllowed: Boolean = pwdGenConfig.duplicateCharsAllowed

  lazy val defaultPasswordHash: String = hashPassword(defaultPassword)

  def hashPassword(password: String): String = {
    val hashed: Array[Byte] = hmacUtils.hmac(password)
    // javax.xml.bind.DatatypeConverter (used here previously) was removed from the JDK in
    // Java 11+; replicate its printHexBinary behaviour (uppercase, 2 hex digits per byte, no
    // separators) manually to keep existing password hashes valid.
    hashed.map(b => f"${b & 0xff}%02X").mkString
  }
}
