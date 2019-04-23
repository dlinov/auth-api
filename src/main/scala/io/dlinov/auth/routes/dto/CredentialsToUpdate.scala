package io.dlinov.auth.routes.dto

case class CredentialsToUpdate(user: String, oldPassword: String, newPassword: String)
