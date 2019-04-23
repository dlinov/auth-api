package io.dlinov.auth.routes.dto

final case class LoginResponse(token: String, user: BackOfficeUserToRead)
