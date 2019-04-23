package io.dlinov.auth

import java.util.concurrent.Executors

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import com.softwaremill.macwire._
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import org.http4s.rho.AuthedContext
import org.http4s.rho.bits.PathAST.{PathMatch, TypedPath}
import org.http4s.rho.swagger.SwaggerSupport
import org.http4s.rho.swagger.models.{ApiKeyAuthDefinition, In, Info, SecurityRequirement}
import org.http4s.server.middleware._
import org.slf4j.LoggerFactory
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.couchbase.BlobCouchbaseDao
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, BlobFDao, BlobTmpFDao, BusinessUnitFDao, DocumentFDao, RoleFDao, ScopeFDao}
import io.dlinov.auth.dao.hdfs.BlobHdfsDao
import io.dlinov.auth.dao.hikari.{BackOfficeUserHikariDao, BusinessUnitHikariDao, DocumentHikariDao, HikariDBFApi, PermissionHikariDao, RoleHikariDao, ScopeHikariDao}
import io.dlinov.auth.dao.hikari.ec.{BlockingECWrapper, ConnectECWrapper, TransactECWrapper}
import io.dlinov.auth.domain.algebras.{AuthenticationAlgebra, BackOfficeUserAlgebra, BusinessUnitAlgebra, DocumentAlgebra, PasswordAlgebra, PermissionAlgebra, RoleAlgebra, ScopeAlgebra}
import io.dlinov.auth.domain.algebras.services.{CaptchaService, EmailNotificationService, NotificationService}
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.{AdminRoutes, AuthenticationRoutes, BackOfficeUserRoutes, BusinessUnitRoutes, HealthCheckRoutes, PermissionRoutes, RoleRoutes, ScopeRoutes, StaticsRoutes}
import io.dlinov.auth.routes.proxied.{AccountsProxy, TransactionsProxy, TypesProxy}

import scala.concurrent.ExecutionContext

trait Bootable extends AuthenticationMiddlewareProvider {
  protected implicit def contextShift: ContextShift[IO]
  protected implicit def timer: Timer[IO]

  lazy val appConfig: AppConfig = AppConfig.load
  lazy val dbConfig: AppConfig.DbConfig = appConfig.db
  lazy val authConfig: AppConfig.AuthConfig = appConfig.auth
  lazy val loggerConfig: AppConfig.LogConfig = appConfig.logging
  lazy val emailConfig: AppConfig.EmailConfig = appConfig.email
  lazy val couchbaseConfig: AppConfig.CouchbaseConfig = appConfig.couchbase
  lazy val hdfsConfig: AppConfig.HdfsConfig = appConfig.hdfs
  lazy val proxyConfig: AppConfig.ProxyConfig = appConfig.proxy
  val connectEC = new ConnectECWrapper(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
  val transactEC = new TransactECWrapper(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
  // Static file support uses a blocking API, so we’ll need a blocking execution context:
  val webJarsEc = new BlockingECWrapper(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4)))
  implicit val httpClientEC: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  lazy val httpClientResource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](httpClientEC).resource

  lazy val dbApi: DBFApi[IO] = wire[HikariDBFApi]
  lazy val scopeDao: ScopeFDao = wire[ScopeHikariDao]
  lazy val roleDao: RoleFDao = wire[RoleHikariDao]
  lazy val buDao: BusinessUnitFDao = wire[BusinessUnitHikariDao]
  lazy val permissionDao: PermissionHikariDao = wire[PermissionHikariDao]
  lazy val backOfficeUserDao: BackOfficeUserFDao = wire[BackOfficeUserHikariDao]
  lazy val docDao: DocumentFDao = wire[DocumentHikariDao]
  lazy val blobHdfsDao: BlobFDao = wire[BlobHdfsDao]
  lazy val blobCouchbaseDao: BlobTmpFDao = wire[BlobCouchbaseDao]

  lazy val captchaService: CaptchaService = wire[CaptchaService]
  lazy val notificationService: NotificationService = wire[EmailNotificationService]
  lazy val passwordAlgebra: PasswordAlgebra = wire[PasswordAlgebra]
  lazy val businessUnitAlgebra: BusinessUnitAlgebra = wire[BusinessUnitAlgebra]
  lazy val roleAlgebra: RoleAlgebra = wire[RoleAlgebra]
  lazy val scopeService: ScopeAlgebra = wire[ScopeAlgebra]
  lazy val backOfficeUserAlgebra: BackOfficeUserAlgebra = wire[BackOfficeUserAlgebra]
  lazy val permissionAlgebra: PermissionAlgebra = wire[PermissionAlgebra]
  lazy val authenticationAlgebra: AuthenticationAlgebra = wire[AuthenticationAlgebra]
  lazy val documentAlgebra: DocumentAlgebra = wire[DocumentAlgebra]

  val auth = new AuthedContext[IO, BackOfficeUser]

  private lazy val healthCheckRoutes = wire[HealthCheckRoutes]
  private lazy val adminRoutes = wire[AdminRoutes]
  private lazy val authenticationRoutes = wire[AuthenticationRoutes]
  private lazy val bouRoutes = wire[BackOfficeUserRoutes]
  private lazy val buRoutes = wire[BusinessUnitRoutes]
  private lazy val roleRoutes = wire[RoleRoutes]
  private lazy val permissionRoutes = wire[PermissionRoutes]
  private lazy val scopesRoutes = wire[ScopeRoutes]
  private lazy val accountsProxy = wire[AccountsProxy]
  private lazy val txnsProxy = wire[TransactionsProxy]
  private lazy val typesProxy = wire[TypesProxy]
  private lazy val staticsRoutes = wire[StaticsRoutes[IO]]

  private val apiContentTypes = List("application/json")
  private val swaggerAnonMiddleware = SwaggerSupport[IO].createRhoMiddleware(
    swaggerFormats = org.http4s.rho.swagger.DefaultSwaggerFormats, // SwaggerFormats,
    apiPath = TypedPath(PathMatch(AuthenticationMiddlewareProvider.SwaggerAnonPath)),
    apiInfo = Info(title = "Authentication API (ANONYMOUS PART)", version = BuildInfo.version),
    consumes = apiContentTypes,
    produces = apiContentTypes)
  private val swaggerMiddleware = SwaggerSupport[IO].createRhoMiddleware(
    apiInfo = Info(title = "Authentication API", version = BuildInfo.version),
    consumes = apiContentTypes,
    produces = apiContentTypes,
    security = List(SecurityRequirement("Bearer", List.empty[String])),
    securityDefinitions = Map(
      "Bearer" → ApiKeyAuthDefinition("Authorization", In.HEADER)))

  val services: HttpApp[IO] = {
    val anonymousRoutes = NonEmptyList
      .of(authenticationRoutes, healthCheckRoutes, typesProxy)
      .map(_.routes)
      .reduceLeft(_ and _)
      .toRoutes(swaggerAnonMiddleware)
    val authenticatedRoutes = Authenticated(auth.toService(NonEmptyList
      .of(scopesRoutes, bouRoutes, buRoutes, roleRoutes, permissionRoutes, accountsProxy, txnsProxy, adminRoutes)
      .map(_.routes)
      .reduceLeft(_ and _)
      .toRoutes(swaggerMiddleware)))
    val logAction: String ⇒ IO[Unit] = {
      if (loggerConfig.isEnabled) {
        val logger = LoggerFactory.getLogger("request-response")
        msg ⇒ IO(logger.debug(msg))
      } else {
        _ ⇒ IO.unit
      }
    }
    val loggedRoutes = Logger.httpRoutes(
      logHeaders = loggerConfig.logHeaders, logBody = loggerConfig.logBody, logAction = Some(logAction))(
      anonymousRoutes <+> authenticatedRoutes)
    val routesConcatenated: HttpRoutes[IO] = staticsRoutes.routes <+> loggedRoutes
    GZip(routesConcatenated).orNotFound
  }
}
