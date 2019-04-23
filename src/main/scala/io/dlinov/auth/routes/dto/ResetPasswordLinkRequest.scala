package io.dlinov.auth.routes.dto

import io.dlinov.auth.domain.auth.entities.Email

final case class ResetPasswordLinkRequest(userName: String, email: Email, captcha: Option[String])
