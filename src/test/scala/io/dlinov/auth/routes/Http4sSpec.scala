package io.dlinov.auth.routes

import java.util.UUID
import java.util.concurrent.TimeoutException

import cats.Applicative
import cats.data.{EitherT, OptionT}
import cats.effect.{Async, IO}
import cats.effect.unsafe.implicits.global
import com.zaxxer.hikari.HikariDataSource
import io.dlinov.auth.dao.DaoError
import io.dlinov.auth.dao.hikari.HikariDBFApi
import io.dlinov.auth.dao.hikari.ec.ConnectECWrapper
import io.dlinov.auth.domain.algebras.services.NotificationService
import io.dlinov.auth.{AppConfig, Bootable, TokenBehavior}
import io.dlinov.auth.domain.auth.entities.{
  BackOfficeUser,
  ClaimContent,
  Email,
  Notification,
  Scope,
  Scopes
}
import io.dlinov.auth.routes.dto.BackOfficeUserToCreate
import io.dlinov.auth.util.Logging
import monocle.syntax.all._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import io.dlinov.auth.routes.Http4sSpec.InitialData
import io.dlinov.auth.dao.Dao.DaoResponse
import io.dlinov.auth.routes.json.CirceEncoders.claimContentEncoder
import io.dlinov.auth.routes.dto.PermissionKeys.UserPermissionKey
import io.dlinov.auth.routes.json.{EntityDecoders, EntityEncoders}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.util.Random

trait Http4sSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with BeforeAndAfterAll
    with Http4sSpecHelper
    with Bootable[IO]
    with Logging
    with EntityDecoders[IO]
    with EntityEncoders[IO] {

  implicit override protected def asyncF: Async[IO]             = IO.asyncForIO
  implicit override protected def applicativeF: Applicative[IO] = asyncF

  override lazy val appConfig: AppConfig = {
    val config       = AppConfig.load
    val dbSuffix     = Random.nextInt(Short.MaxValue)
    val pimpedConfig = config
      .focus(_.db.url)
      .modify(_.replace("h2:mem:myapp_db;", s"h2:mem:myapp_db_$dbSuffix;"))
    logger.info(s"Connection string: ${pimpedConfig.db.url}")
    pimpedConfig
  }

  // The Hikari datasource, its bounded connect executor and the Ember http client are each
  // resources with a suite-scoped lifetime: acquired once in `beforeAll` (mirroring what
  // CatsIOBoot does for the whole application) and released once in `afterAll`, rather than
  // being self-constructed/never-closed `lazy val`s as they used to be on `Bootable` itself.
  private var releaseResources: IO[Unit]                    = IO.unit
  private var _hikariDataSource: HikariDataSource           = scala.compiletime.uninitialized
  private var _connectEC: ConnectECWrapper                  = scala.compiletime.uninitialized
  private var _httpClient: Client[IO]                       = scala.compiletime.uninitialized
  override protected def hikariDataSource: HikariDataSource =
    _hikariDataSource
  override protected def connectEC: ConnectECWrapper = _connectEC
  override protected def httpClient: Client[IO]      = _httpClient

  override lazy val notificationService: NotificationService[IO] =
    (notification: Notification) => IO(Right(notificationsBuffer += notification))

  protected val defaultCreatedBy                               = "system"
  protected val scopesScope                                    = new Scopes("scopes")
  protected val rolesScope                                     = new Scopes("roles")
  protected val buScope                                        = new Scopes("business_units")
  protected val userScopes                                     = new Scopes("back_office_users")
  protected val permissionScopes                               = new Scopes("permissions")
  protected val typesScope                                     = new Scopes("types")
  protected val notificationsBuffer: ArrayBuffer[Notification] =
    mutable.ArrayBuffer.empty[Notification]
  protected var initialData: InitialData = scala.compiletime.uninitialized

  // idea is to locate all preparation side effects in this method
  override def beforeAll(): Unit = {
    super.beforeAll()
    val resources = for {
      ds     <- HikariDBFApi.dataSourceResource[IO](appConfig.db)
      ec     <- ConnectECWrapper.resource[IO](appConfig.db.poolSize)
      client <- EmberClientBuilder.default[IO].build
    } yield (ds, ec, client)
    val ((ds, ec, client), release) = resources.allocated.unsafeRunSync()
    _hikariDataSource = ds
    _connectEC = ec
    _httpClient = client
    releaseResources = release
    initialData = initData().unsafeRunSync().toOption.get
  }

  override def afterAll(): Unit = {
    releaseResources.unsafeRunSync()
    super.afterAll()
  }

  // final modifier is TBD
  final protected def initData(): IO[DaoResponse[InitialData]] = {
    val insScope: (String, Option[UUID]) => EitherT[IO, DaoError, Scope] =
      (name, parentId) =>
        EitherT {
          scopeDao.create(
            name = name,
            parentId = parentId,
            description = None,
            createdBy = defaultCreatedBy,
            reactivate = false
          )
        }
    (for {
      // create business unit
      bu <- EitherT(
        buDao.create("buStub".toUpperCase, createdBy = defaultCreatedBy, reactivate = false)
      )
      // create role
      r <- EitherT(
        roleDao.create("rStub".toUpperCase, createdBy = defaultCreatedBy, reactivate = false)
      )
      // create scopes
      scopeParent <- insScope(scopesScope.parent, None)
      scopeParentId = Some(scopeParent.id)
      scopeCreate <- insScope(scopesScope.create, scopeParentId)
      scopeUpdate <- insScope(scopesScope.update, scopeParentId)
      scopeDetail <- insScope(scopesScope.detail, scopeParentId)
      roleParent  <- insScope(rolesScope.parent, None)
      roleParentId = Some(roleParent.id)
      roleCreate <- insScope(rolesScope.create, roleParentId)
      roleUpdate <- insScope(rolesScope.update, roleParentId)
      roleDetail <- insScope(rolesScope.detail, roleParentId)
      buParent   <- insScope(buScope.parent, None)
      buParentId = Some(buParent.id)
      buCreate   <- insScope(buScope.create, buParentId)
      buUpdate   <- insScope(buScope.update, buParentId)
      buDetail   <- insScope(buScope.detail, buParentId)
      userParent <- insScope(userScopes.parent, None)
      userParentId = Some(userParent.id)
      userCreate       <- insScope(userScopes.create, userParentId)
      userUpdate       <- insScope(userScopes.update, userParentId)
      userDetail       <- insScope(userScopes.detail, userParentId)
      permissionParent <- insScope(permissionScopes.parent, None)
      permissionParentId = Some(permissionParent.id)
      permissionCreate <- insScope(permissionScopes.create, permissionParentId)
      typesParent      <- insScope(typesScope.parent, None)
      // create user
      user <- EitherT(
        backOfficeUserDao.create(
          userName = "admin",
          password = "password",
          email = Email("admin@foo.bar"),
          phoneNumber = None,
          firstName = "Thomas",
          middleName = Some("A."),
          lastName = "Anderson",
          description = None,
          homePage = None,
          activeLanguage = None,
          customData = None,
          roleId = r.id,
          businessUnitId = bu.id,
          createdBy = defaultCreatedBy,
          reactivate = false
        )
      )
      // create permissions
      grantPermission = (scopeId: UUID) =>
        EitherT {
          permissionDao.create(
            pKey = UserPermissionKey(user.id),
            scopeId = scopeId,
            revoke = false,
            createdBy = defaultCreatedBy,
            reactivate = false
          )
        }
      _ <- grantPermission(scopeParent.id)
      _ <- grantPermission(scopeCreate.id)
      _ <- grantPermission(scopeUpdate.id)
      _ <- grantPermission(scopeDetail.id)
      _ <- grantPermission(roleParent.id)
      _ <- grantPermission(roleCreate.id)
      _ <- grantPermission(roleUpdate.id)
      _ <- grantPermission(roleDetail.id)
      _ <- grantPermission(buParent.id)
      _ <- grantPermission(buCreate.id)
      _ <- grantPermission(buUpdate.id)
      _ <- grantPermission(buDetail.id)
      _ <- grantPermission(userParent.id)
      _ <- grantPermission(userCreate.id)
      _ <- grantPermission(userUpdate.id)
      _ <- grantPermission(userDetail.id)
      _ <- grantPermission(permissionParent.id)
      _ <- grantPermission(permissionCreate.id)
      _ <- grantPermission(typesParent.id)
    } yield {
      val gen = new TokenBehavior {
        override def tokenExpirationInMinutes: Int = 60
      }
      val token = gen.generateTokenCirce("admin", ClaimContent.from(user))
      InitialData(user, token, r.id, bu.id)
    }).value
  }

  /** Polls `notificationsBuffer` for a new notification, bounded by `deadline`/`maxAttempts` so a
    * test fails clearly (rather than hanging the test run/thread forever) if the expected
    * notification never arrives.
    */
  @tailrec
  final protected def fetchNextNotification(
      mbLastNotification: Option[Notification],
      deadline: Deadline = 10.seconds.fromNow
  ): Option[Notification] = {
    val currentMaybeLastNotification = notificationsBuffer.lastOption
    if (currentMaybeLastNotification == mbLastNotification) {
      if (deadline.isOverdue()) {
        throw new TimeoutException(
          "Timed out after 10 seconds waiting for a new notification to be published"
        )
      }
      Thread.sleep(200)
      fetchNextNotification(mbLastNotification, deadline)
    } else {
      currentMaybeLastNotification
    }
  }

  protected def registerUser(
      userToCreate: BackOfficeUserToCreate,
      token: String
  ): IO[Option[String]] = {
    val passwordRegex = "Password: (.*)".r
    val request       = buildPostRequest[BackOfficeUserToCreate](
      uri = "/api/back_office_users",
      entity = userToCreate,
      token = token
    )
    (for {
      mbLastNotification <- OptionT.liftF(IO(notificationsBuffer.lastOption))
      resp               <- OptionT.liftF(services.run(request))
      _ <- OptionT.liftF(IO(logger.info(s"Registration response status: ${resp.status}")))
      passwordNotification <- OptionT(IO(fetchNextNotification(mbLastNotification)))
      _                    <- OptionT.liftF(IO(logger.warn(passwordNotification.message)))
      pwdMatch <- OptionT(IO(passwordRegex.findFirstMatchIn(passwordNotification.message)))
    } yield {
      pwdMatch.group(1)
    }).value
  }
}

object Http4sSpec {
  case class InitialData(
      superAdmin: BackOfficeUser,
      superAdminToken: String,
      defaultRoleId: UUID,
      defaultBusinessUnitId: UUID
  )
}
