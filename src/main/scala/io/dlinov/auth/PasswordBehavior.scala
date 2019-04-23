package io.dlinov.auth

import cats.data.NonEmptyList

import scala.annotation.tailrec
import scala.util.Random

trait PasswordBehavior {
  import PasswordBehavior._

  def nDigits: Int
  def nLowercase: Int
  def nUppercase: Int
  def nSpecial: Int
  def minPasswordLength: Int
  def duplicateCharAllowed: Boolean
  protected val defaultPassword: String

  if (minPasswordLength < (nDigits + nUppercase + nSpecial)) {
    throw new RuntimeException("configured password length is too short to comply with password strength rules")
  }

  def generatePassword: String = {
    Random.shuffle((getNLowercase(minPasswordLength).drop(nDigits + nUppercase + nSpecial) +
      getNDigits(nDigits) + getNUppercase(nUppercase) + getNSpecialCharacters(nSpecial)).toList).mkString
  }

  def validatePassword(
    oldPassword: Option[String],
    password: String): Either[NonEmptyList[String], String] = {
    Some(password) match {
      case `oldPassword` ⇒
        Left(NonEmptyList.one("New password cannot be the same as old password"))
      case Some(`defaultPassword`) ⇒
        Left(NonEmptyList.one("New password cannot be the same as default password"))
      case _ ⇒
        val errors = validatePassword(password)
        NonEmptyList.fromList(errors).toLeft(password)
    }
  }

  // TODO: use cats validation classes
  def validatePassword(password: String): List[String] = {
    val validationErrors = scala.collection.mutable.ListBuffer[String]()

    if (password == null || password.trim.isEmpty) {
      validationErrors.append(PEmpty)
    } else {
      if (password.length < minPasswordLength) {
        validationErrors.append(MinPLength)
      }

      val passwordChars = password.toCharArray.toSeq
      if (passwordChars.count(lowercaseCharsSet.contains) < nLowercase) {
        validationErrors.append(NotEnoughLowercase)
      }
      if (passwordChars.count(uppercaseCharsSet.contains) < nUppercase) {
        validationErrors.append(NotEnoughUppercase)
      }
      if (passwordChars.count(specialCharsSet.contains) < nSpecial) {
        validationErrors.append(NotEnoughSpecialChars)
      }
      if (passwordChars.count(digitsSet.contains) < nDigits) {
        validationErrors.append(NotEnoughDigits)
      }
    }

    validationErrors.toList
  }

  private def getNChars(n: Int, chars: Seq[Char]): String = {
    @tailrec def getNCharsInternal(nRemaining: Int, src: Seq[Char], acc: Seq[Char]): String = {
      if (nRemaining > 0) {
        val nextChar = src(Random.nextInt(src.size))
        val nextSrc = if (duplicateCharAllowed) src else src.filterNot(_ == nextChar)
        getNCharsInternal(nRemaining - 1, nextSrc, acc :+ nextChar)
      } else {
        acc.mkString
      }
    }

    if (!duplicateCharAllowed && chars.size < n) {
      throw new IllegalArgumentException(s"Char source has less elems than distinct elems requested")
    }
    getNCharsInternal(n, chars, Seq.empty)
  }

  def getNDigits(n: Int): String = {
    getNChars(n, digits)
  }

  def getNSpecialCharacters(n: Int): String = {
    getNChars(n, specialChars)
  }

  def getNLowercase(n: Int): String = {
    getNChars(n, lowercaseChars)
  }

  def getNUppercase(n: Int): String = {
    getNChars(n, uppercaseChars)
  }
}

object PasswordBehavior {
  val MinPLength = "Minimum password length not satisfied"
  val NotEnoughLowercase = "Not enough lowercase characters in the password"
  val NotEnoughUppercase = "Not enough uppercase characters in the password"
  val NotEnoughSpecialChars = "Not enough special characters in the password"
  val NotEnoughDigits = "Not enough numeric characters in the password"
  val PEmpty = "Password cannot be null or empty"

  private val digits = '0' to '9'
  private val digitsSet = digits.toSet
  private val lowercaseChars = 'a' to 'z'
  private val lowercaseCharsSet = lowercaseChars.toSet
  private val uppercaseChars = 'A' to 'Z'
  private val uppercaseCharsSet = uppercaseChars.toSet
  // I can't see any reasons to exclude, but originally there was none of these
  private val specialExcludedChars = Set('.', '\\', '|', '`', ',', '\'', ':')
  // "~!@#$%^&*()-_+=/[]{}<>?;\"".toCharArray
  private val specialCharsSet = ('!' to '}').toSet.--(digitsSet.|(lowercaseCharsSet).|(uppercaseCharsSet).|(specialExcludedChars))
  private val specialChars = specialCharsSet.toSeq
}
