package io.dlinov.auth.routes.dto

final case class CredentialsToRead(
    user: String,
    password: String,
    captcha: Option[String])
