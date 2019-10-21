package io.dlinov.auth.util

import org.apache.commons.text.StringEscapeUtils

object Implicits {
  implicit class Escaper(val arg: String) extends AnyVal {
    def escapeJava: String = {
      StringEscapeUtils.escapeJava(arg)
    }
  }

  private val replaceAll = (arg: String) ⇒ {
    arg
      .replace("""'""", "")
      .replace(""""""", "")
      .replace("""%27""", "")
      .replace("""\0""", "")
      .replace("""\b""", "")
      .replace("""\n""", "")
      .replace("""\r""", "")
      .replace("""\t""", "")
      .replace("""\Z""", "")
      .replace("""\_""", "")
      .replace("""%""", "")
      .replace("""\z1a""", "")
      .replaceAll("""<(|\/|[^\/>][^>]+|\/[^>][^>]+)>""", "")
      .replaceAll("""<[^>]*>""", "")
      .replace("""\""", "")
      .replace("""{""", "")
      .replace("""}""", "")
  }

  implicit class Sanitizer(val arg: String) extends AnyVal {
    def sanitize: String = {
      replaceAll(arg)
    }
  }

  implicit class RichString(arg: String) {
    def hasSomething = {
      arg != null && arg.trim.nonEmpty
    }
  }
}
