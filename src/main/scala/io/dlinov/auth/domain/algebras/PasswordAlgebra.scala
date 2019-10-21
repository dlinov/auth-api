package io.dlinov.auth.domain.algebras

import java.util.Base64

import io.dlinov.auth.{AppConfig, PasswordBehavior}
import javax.xml.bind.DatatypeConverter
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}
import io.dlinov.auth.PasswordBehavior

class PasswordAlgebra(config: AppConfig) extends PasswordBehavior {

  private lazy val authConfig   = config.auth
  private lazy val pwdGenConfig = authConfig.passwordGeneration
  private lazy val secretKey: String =
    Base64.getEncoder.encodeToString(authConfig.secret.reverse.getBytes)
  private lazy val hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey)

  override protected lazy val defaultPassword: String = authConfig.passwordGeneration.default

  override lazy val nDigits: Int             = pwdGenConfig.nDigits
  override lazy val nLowercase: Int          = pwdGenConfig.nLowercase
  override lazy val nUppercase: Int          = pwdGenConfig.nUppercase
  override lazy val nSpecial: Int            = pwdGenConfig.nSpecialChars
  override lazy val minPasswordLength: Int   = pwdGenConfig.length
  override val duplicateCharAllowed: Boolean = pwdGenConfig.duplicateCharsAllowed

  lazy val defaultPasswordHash: String = hashPassword(defaultPassword)

  def hashPassword(password: String): String = {
    val hashed: Array[Byte] = hmacUtils.hmac(password)
    DatatypeConverter.printHexBinary(hashed)
  }
}
