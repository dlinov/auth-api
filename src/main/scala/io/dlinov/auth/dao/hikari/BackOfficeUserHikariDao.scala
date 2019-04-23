package io.dlinov.auth.dao.hikari

import java.time.ZonedDateTime
import java.util.UUID

import cats.data.OptionT
import cats.effect.IO
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._
import doobie._
import doobie.implicits._
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.BackOfficeUserFDao
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.{BackOfficeUser, BusinessUnit, Email, Permission, Role}
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.domain.auth.entities._
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.BackOfficeUserFDao
import io.dlinov.auth.domain.PaginatedResult

import scala.collection.mutable.ArrayBuffer

class BackOfficeUserHikariDao(
    db: DBFApi[IO],
    permissionDao: PermissionHikariDao)
  extends BackOfficeUserFDao {

  import HikariDBFApi._
  import BackOfficeUserHikariDao._

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
    reactivate: Boolean): IO[DaoResponse[BackOfficeUser]] = {
    for {
      xa ← db.transactor
      result ← (if (reactivate) {
        reactivateInternal(
          userName = userName,
          password = password,
          email = email,
          phoneNumber = phoneNumber,
          firstName = firstName,
          middleName = middleName,
          lastName = lastName,
          description = description,
          homePage = homePage,
          activeLanguage = activeLanguage,
          customData = customData,
          roleId = roleId,
          businessUnitId = businessUnitId,
          createdBy = createdBy)
      } else {
        createInternal(
          userName = userName,
          password = password,
          email = email,
          phoneNumber = phoneNumber,
          firstName = firstName,
          middleName = middleName,
          lastName = lastName,
          description = description,
          homePage = homePage,
          activeLanguage = activeLanguage,
          customData = customData,
          roleId = roleId,
          businessUnitId = businessUnitId,
          createdBy = createdBy)
      }).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .create($userName,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findById(id: UUID): IO[DaoResponse[Option[BackOfficeUser]]] = {
    for {
      xa ← db.transactor
      result ← findByIdInternal(id).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findById($id): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findByName(name: String): IO[DaoResponse[Option[BackOfficeUser]]] = {
    for {
      xa ← db.transactor
      result ← findByNameInternal(name).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findByName($name): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def findAll(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int],
    maybeFirstName: Option[String],
    maybeLastName: Option[String],
    maybeEmail: Option[String],
    maybePhoneNumber: Option[String]): IO[DaoResponse[PaginatedResult[BackOfficeUser]]] = {
    for {
      xa ← db.transactor
      result ← findAllInternal(
        maybeLimit = maybeLimit,
        maybeOffset = maybeOffset,
        maybeFirstName = maybeFirstName,
        maybeLastName = maybeLastName,
        maybeEmail = maybeEmail,
        maybePhoneNumber = maybePhoneNumber).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .findAll: " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def countActiveByRoleId(roleId: UUID): IO[DaoResponse[Int]] = {
    for {
      xa ← db.transactor
      result ← countActiveByRoleIdInternal(roleId).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .countActiveByRoleId($roleId): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def countActiveByBusinessUnitId(buId: UUID): IO[DaoResponse[Int]] = {
    for {
      xa ← db.transactor
      result ← countActiveByBusinessUnitIdInternal(buId).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .countActiveByBusinessUnitId($buId): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def update(
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
    updatedBy: String): IO[DaoResponse[Option[BackOfficeUser]]] = {
    for {
      xa ← db.transactor
      result ← updateInternal(
        id = id,
        email = email,
        phoneNumber = phoneNumber,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        description = description,
        homePage = homePage,
        activeLanguage = activeLanguage,
        customData = customData,
        roleId = roleId,
        businessUnitId = businessUnitId,
        updatedBy = updatedBy).transact(xa).attempt
    } yield result.leftMap { exc ⇒
      val msg = s"Unexpected error in .update($id,..): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  def remove(id: UUID, updatedBy: String): IO[DaoResponse[Option[BackOfficeUser]]] = {
    for {
      xa ← db.transactor
      maybeUser ← (for {
        user ← OptionT(findByIdInternal(id))
        _ ← OptionT.liftF(removeQuery(id, updatedBy).update.run)
      } yield user).value.transact(xa).attempt
    } yield maybeUser.leftMap { exc ⇒
      val msg = s"Unexpected error in .remove($id, $updatedBy): " + exc.getMessage
      logger.warn(msg, exc)
      genericDbError(msg)
    }
  }

  override def login(name: String, passwordHash: String): IO[Option[BackOfficeUser]] = {
    for {
      xa ← db.transactor
      result ← loginInternal(name, passwordHash).transact(xa)
    } yield result
  }

  override def updatePassword(
    name: String,
    oldPasswordHash: String,
    passwordHash: String): IO[Option[BackOfficeUser]] = {
    for {
      xa ← db.transactor
      result ← (for {
        _ ← updatePasswordInternal(name, oldPasswordHash, passwordHash)
        maybeUser ← loginInternal(name, passwordHash)
      } yield maybeUser).transact(xa)
    } yield result
  }

  override def resetPassword(name: String, passwordHash: String): IO[Option[BackOfficeUser]] = {
    for {
      xa ← db.transactor
      result ← (for {
        _ ← resetPasswordInternal(name, passwordHash)
        maybeUser ← loginInternal(name, passwordHash)
      } yield maybeUser).transact(xa)
    } yield result
  }

  private[hikari] def createInternal(
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
    createdBy: String): ConnectionIO[BackOfficeUser] = {
    val id = UUID.randomUUID()
    for {
      _ ← insertQuery(
        id = id,
        userName = userName,
        password = password,
        email = email,
        phoneNumber = phoneNumber,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        description = description,
        homePage = homePage,
        activeLanguage = activeLanguage,
        customData = customData,
        roleId = roleId,
        businessUnitId = businessUnitId,
        cBy = createdBy).update.run
      maybeUser ← fetchByIdInternal(id)
    } yield maybeUser
  }

  private[hikari] def reactivateInternal(
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
    createdBy: String): ConnectionIO[BackOfficeUser] = {
    for {
      maybeExistingUser ← findByNameInternal(userName)
      upsertedUser ← maybeExistingUser.fold {
        createInternal(
          userName = userName,
          password = password,
          email = email,
          phoneNumber = phoneNumber,
          firstName = firstName,
          middleName = middleName,
          lastName = lastName,
          description = description,
          homePage = homePage,
          activeLanguage = activeLanguage,
          customData = customData,
          roleId = roleId,
          businessUnitId = businessUnitId,
          createdBy = createdBy)
      } { _ ⇒
        for {
          _ ← reactivateQuery(
            userName = userName,
            password = password,
            email = email,
            phoneNumber = phoneNumber,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            description = description,
            homePage = homePage,
            activeLanguage = activeLanguage,
            customData = customData,
            roleId = roleId,
            businessUnitId = businessUnitId,
            createdBy = createdBy).update.run
          reactivatedUser ← fetchByNameInternal(userName)
        } yield reactivatedUser
      }
    } yield upsertedUser
  }

  private[hikari] def findByIdInternal(id: UUID): ConnectionIO[Option[BackOfficeUser]] = {
    findOneInternal(queryById(id))
  }

  private[hikari] def findByNameInternal(name: String): ConnectionIO[Option[BackOfficeUser]] = {
    findOneInternal(queryByName(name))
  }

  private[hikari] def findAllInternal(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int],
    maybeFirstName: Option[String],
    maybeLastName: Option[String],
    maybeEmail: Option[String],
    maybePhoneNumber: Option[String]): ConnectionIO[PaginatedResult[BackOfficeUser]] = {
    for {
      page ← queryAll(
        maybeLimit,
        maybeOffset,
        maybeFirstName = maybeFirstName,
        maybeLastName = maybeLastName,
        maybeEmail = maybeEmail,
        maybePhoneNumber = maybePhoneNumber).query[BOUserHikari].to[List]
      total ← countAll.query[Int].unique
      users ← page.map { user ⇒
        permissionDao.findAndMergeInternal(
          businessUnitId = user.businessUnit.id,
          roleId = user.role.id,
          maybeUserId = Some(user.id),
          maybeLimit = None,
          maybeOffset = None)
          .map(permissionsPage ⇒ user.asDomain(permissionsPage.results))
      }.sequence
    } yield PaginatedResult(total, users, maybeLimit.getOrElse(Int.MaxValue), maybeOffset.getOrElse(0))
  }

  private[hikari] def fetchByIdInternal(id: UUID): ConnectionIO[BackOfficeUser] = {
    fetchOneInternal(queryById(id))
  }

  private[hikari] def fetchByNameInternal(name: String): ConnectionIO[BackOfficeUser] = {
    fetchOneInternal(queryByName(name))
  }

  private[hikari] def findByNameAndPasswordHashInternal(
    name: String,
    hashedPassword: String): ConnectionIO[Option[BackOfficeUser]] = {
    findOneInternal(queryByNameAndPasswordHash(name, hashedPassword))
  }

  private[hikari] def countActiveByRoleIdInternal(roleId: UUID): doobie.ConnectionIO[Int] = {
    (countAll ++ fr"AND roleId = $roleId").query[Int].unique
  }

  private[hikari] def countActiveByBusinessUnitIdInternal(buId: UUID): doobie.ConnectionIO[Int] = {
    (countAll ++ fr"AND businessUnitId = $buId").query[Int].unique
  }

  private[hikari] def updateInternal(
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
    updatedBy: String): ConnectionIO[Option[BackOfficeUser]] = {
    for {
      _ ← updateQuery(
        id = id,
        email = email,
        phoneNumber = phoneNumber,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        description = description,
        homePage = homePage,
        activeLanguage = activeLanguage,
        customData = customData,
        roleId = roleId,
        buId = businessUnitId,
        updatedBy = updatedBy).update.run
      maybeUser ← findByIdInternal(id)
    } yield maybeUser
  }

  private[hikari] def updateLastLoginTimestampInternal(
    name: String,
    passwordHash: String): ConnectionIO[Int] = {
    updateLastLoginTimestampQuery(name, passwordHash).update.run
  }

  private[hikari] def findOneInternal(qFragment: Fragment): ConnectionIO[Option[BackOfficeUser]] = {
    (for {
      user ← OptionT(qFragment.query[BOUserHikari].option)
      permissions ← OptionT.liftF(permissionDao.findAndMergeInternal(
        businessUnitId = user.businessUnit.id,
        roleId = user.role.id,
        maybeUserId = Some(user.id),
        maybeLimit = None,
        maybeOffset = None))
    } yield user.asDomain(permissions.results)).value
  }

  private[hikari] def fetchOneInternal(qFragment: Fragment): ConnectionIO[BackOfficeUser] = {
    for {
      user ← qFragment.query[BOUserHikari].unique
      permissions ← permissionDao.findAndMergeInternal(
        businessUnitId = user.businessUnit.id,
        roleId = user.role.id,
        maybeUserId = Some(user.id),
        maybeLimit = None,
        maybeOffset = None)
    } yield user.asDomain(permissions.results)
  }

  private def loginInternal(name: String, passwordHash: String): ConnectionIO[Option[BackOfficeUser]] = {
    for {
      _ ← updateLastLoginTimestampInternal(name, passwordHash)
      user ← findByNameAndPasswordHashInternal(name, passwordHash)
    } yield user
  }

  private def updatePasswordInternal(
    name: String,
    oldPasswordHash: String,
    passwordHash: String): ConnectionIO[Int] = {
    updatePasswordQuery(name, oldPasswordHash, passwordHash).update.run
  }

  private def resetPasswordInternal(name: String, passwordHash: String): ConnectionIO[Int] = {
    resetPasswordQuery(name, passwordHash).update.run
  }
}

object BackOfficeUserHikariDao {
  import HikariDBFApi._

  val TableName: Fragment = Fragment.const("back_office_users")

  val SelectFromTable: Fragment =
    Fragment.const(
      "SELECT u.id, u.userName, u.email, u.phoneNumber, u.firstName, u.middleName, u.lastName, u.description," +
        "u.homePage, u.activeLanguage, u.customData, u.lastLoginTimestamp," +
        "r.id, r.name, r.cBy, r.uBy, r.cDate, r.uDate," +
        "bu.id, bu.name, bu.cBy, bu.uBy, bu.cDate, bu.uDate," +
        "u.cBy, u.uBy, u.cDate, u.uDate FROM ") ++ TableName ++ fr"u" ++
      fr"INNER JOIN" ++ BusinessUnitHikariDao.TableName ++ fr"bu ON u.businessUnitId = bu.id" ++
      fr"INNER JOIN" ++ RoleHikariDao.TableName ++ fr"r ON u.roleId = r.id"

  val SelectCountFromTable: Fragment = Fragment.const("SELECT COUNT(id) FROM ") ++ TableName

  val InsertIntoTable: Fragment = fr"INSERT INTO" ++ TableName ++
    fr"(id, userName, password, email, phoneNumber, firstName, middleName, lastName, description, homePage, status," ++
    fr"activeLanguage, lastLoginTimestamp, customData, roleId, businessUnitId, cBy, uBy) VALUES"

  val UpdateTable: Fragment = fr"UPDATE" ++ TableName

  def whereIdAndActive(id: UUID): Fragment =
    fr"WHERE u.id = $id AND u.status = 1;"

  def whereUserName(name: String): Fragment =
    fr"WHERE u.userName = $name;"

  def whereUserNameAndActive(name: String): Fragment =
    fr"WHERE u.userName = $name AND u.status = 1;"

  def whereUserNameAndPasswordAndActive(name: String, pwdHash: String): Fragment =
    fr"WHERE u.userName = $name AND u.password = $pwdHash AND u.status = 1;"

  def insertQuery(
    id: UUID,
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
    cBy: String): Fragment = {
    val emailValue = email.value
    InsertIntoTable ++ fr"($id, $userName, $password, $emailValue, $phoneNumber, $firstName, $middleName, $lastName," ++
      fr"$description, $homePage, 1, $activeLanguage, NULL, $customData, $roleId, $businessUnitId, $cBy, $cBy)"
  }

  def reactivateQuery(
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
    createdBy: String): Fragment = {
    val emailV = email.value
    // lastLoginTimestamp left unchanged because user is not created from scratch. This might be changed if needed
    UpdateTable ++ fr"SET password = $password, email = $emailV, phoneNumber = $phoneNumber, firstName = $firstName," ++
      fr"middleName = $middleName, lastName = $lastName, description = $description, homePage = $homePage," ++
      fr"status = 1, activeLanguage = $activeLanguage, customData = $customData, roleId = $roleId," ++
      fr"businessUnitId = $businessUnitId, uBy = $createdBy" ++ whereUserName(userName)
  }

  def queryById(id: UUID): Fragment = SelectFromTable ++ fr"WHERE u.id = $id and u.status = 1;"

  def queryByName(name: String): Fragment = SelectFromTable ++ whereUserNameAndActive(name)

  def queryByNameAndPasswordHash(name: String, pwdHash: String): Fragment =
    SelectFromTable ++ whereUserNameAndPasswordAndActive(name, pwdHash)

  def queryAll(
    maybeLimit: Option[Int],
    maybeOffset: Option[Int],
    maybeFirstName: Option[String],
    maybeLastName: Option[String],
    maybeEmail: Option[String],
    maybePhoneNumber: Option[String]): Fragment = {
    val limit = maybeLimit.fold(EmptyFragment)(lmt ⇒ fr"LIMIT $lmt")
    val offset = maybeOffset.fold(EmptyFragment)(off ⇒ fr"OFFSET $off")
    SelectFromTable ++ fr"WHERE u.status = 1" ++ limit ++ offset
  }

  def countAll: Fragment = SelectCountFromTable ++ fr"WHERE status = 1"

  def updateQuery(
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
    buId: Option[UUID],
    updatedBy: String): Fragment = {
    val updStatementsBuffer = new ArrayBuffer[Fragment]()
    email.foreach(e ⇒ updStatementsBuffer += fr"u.email = $e,")
    phoneNumber.foreach(pn ⇒ updStatementsBuffer += fr"u.phoneNumber = $pn,")
    firstName.foreach(fn ⇒ updStatementsBuffer += fr"u.firstName = $fn,")
    middleName.foreach(mn ⇒ updStatementsBuffer += fr"u.middleName = $mn,")
    lastName.foreach(ln ⇒ updStatementsBuffer += fr"u.lastName = $ln,")
    description.foreach(d ⇒ updStatementsBuffer += fr"u.description = $d,")
    homePage.foreach(hp ⇒ updStatementsBuffer += fr"u.homePage = $hp,")
    activeLanguage.foreach(al ⇒ updStatementsBuffer += fr"u.activeLanguage = $al,")
    customData.foreach(cd ⇒ updStatementsBuffer += fr"u.customData = $cd,")
    roleId.foreach(r ⇒ updStatementsBuffer += fr"u.roleId = $r,")
    buId.foreach(bu ⇒ updStatementsBuffer += fr"u.businessUnitId = $bu,")
    val updStatements = updStatementsBuffer.foldLeft(fr"u SET")(_ ++ _) ++ fr"u.uBy = $updatedBy"
    UpdateTable ++ updStatements ++ whereIdAndActive(id)
  }

  def updateLastLoginTimestampQuery(name: String, pwdHash: String): Fragment = {
    val now = System.currentTimeMillis
    UpdateTable ++ fr"u SET u.lastLoginTimestamp = $now" ++ whereUserNameAndPasswordAndActive(name, pwdHash)
  }

  def updatePasswordQuery(name: String, oldPwdHash: String, pwdHash: String): Fragment = {
    UpdateTable ++ fr"u SET u.password = $pwdHash" ++ whereUserNameAndPasswordAndActive(name, oldPwdHash)
  }

  def resetPasswordQuery(name: String, pwdHash: String): Fragment = {
    UpdateTable ++ fr"u SET u.password = $pwdHash" ++ whereUserNameAndActive(name)
  }

  def removeQuery(id: UUID, updatedBy: String): Fragment = {
    UpdateTable ++ fr"u SET u.status = 0, u.uBy = $updatedBy" ++ whereIdAndActive(id)
  }

  case class BOUserHikari(
      id: UUID,
      userName: String,
      email: Email,
      phoneNumber: Option[String],
      firstName: String,
      middleName: Option[String],
      lastName: String,
      description: Option[String],
      homePage: Option[String],
      activeLanguage: Option[String],
      customData: Option[String],
      lastLoginTimestamp: Option[Long],
      role: Role,
      businessUnit: BusinessUnit,
      createdBy: String,
      updatedBy: String,
      createdTime: ZonedDateTime,
      updatedTime: ZonedDateTime)

  implicit class BOUserHikariOps(val u: BOUserHikari) extends AnyVal {
    def asDomain(permissions: Seq[Permission]) =
      BackOfficeUser(
        id = u.id,
        userName = u.userName,
        email = u.email,
        phoneNumber = u.phoneNumber,
        firstName = u.firstName,
        middleName = u.middleName,
        lastName = u.lastName,
        description = u.description,
        homePage = u.homePage,
        activeLanguage = u.activeLanguage,
        customData = u.customData,
        lastLoginTimestamp = u.lastLoginTimestamp,
        role = u.role,
        businessUnit = u.businessUnit,
        permissions = permissions,
        createdBy = u.createdBy,
        updatedBy = u.updatedBy,
        createdTime = u.createdTime,
        updatedTime = u.updatedTime)
  }
}
