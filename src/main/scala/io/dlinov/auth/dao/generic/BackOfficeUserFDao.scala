package io.dlinov.auth.dao.generic

import java.util.UUID

import cats.effect.IO
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Email}
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, Email}
import io.dlinov.auth.dao.Dao
import io.dlinov.auth.domain.PaginatedResult

trait BackOfficeUserFDao extends Dao {
  def create(
    userName: String,
    password: String,
    email: Email,
    phoneNumber: Option[String],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[String],
    roleId: UUID,
    businessUnitId: UUID,
    createdBy: String,
    reactivate: Boolean): IO[DaoResponse[BackOfficeUser]]

  def findById(id: UUID): IO[DaoResponse[Option[BackOfficeUser]]]

  def findByName(name: String): IO[DaoResponse[Option[BackOfficeUser]]]

  def findAll(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int],
    maybeFirstName: Option[String],
    maybeLastName: Option[String],
    maybeEmail: Option[String],
    maybePhoneNumber: Option[String]): IO[DaoResponse[PaginatedResult[BackOfficeUser]]]

  def countActiveByRoleId(roleId: UUID): IO[DaoResponse[Int]]

  def countActiveByBusinessUnitId(buId: UUID): IO[DaoResponse[Int]]

  def update(
    id: UUID,
    email: Option[String],
    phoneNumber: Option[String],
    firstName: Option[String],
    middleName: Option[String],
    lastName: Option[String],
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[String],
    roleId: Option[UUID],
    businessUnitId: Option[UUID],
    updatedBy: String): IO[DaoResponse[Option[BackOfficeUser]]]

  def remove(
    id: UUID,
    updatedBy: String): IO[DaoResponse[Option[BackOfficeUser]]]

  def login(name: String, passwordHash: String): IO[Option[BackOfficeUser]]

  def updatePassword(name: String, oldPasswordHash: String, passwordHash: String): IO[Option[BackOfficeUser]]

  def resetPassword(name: String, passwordHash: String): IO[Option[BackOfficeUser]]
}
