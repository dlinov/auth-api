package io.dlinov.auth

import java.util.concurrent.Executors

import cats.data.NonEmptyList
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Sync, Timer}
import cats.syntax.semigroupk._
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
import io.dlinov.auth.dao.generic.{
  BackOfficeUserFDao,
  BlobFDao,
  BlobTmpFDao,
  BusinessUnitFDao,
  DocumentFDao,
  RoleFDao,
  ScopeFDao
}
import io.dlinov.auth.dao.hdfs.BlobHdfsDao
import io.dlinov.auth.dao.hikari.{
  BackOfficeUserHikariDao,
  BusinessUnitHikariDao,
  DocumentHikariDao,
  HikariDBFApi,
  PermissionHikariDao,
  RoleHikariDao,
  ScopeHikariDao
}
import io.dlinov.auth.dao.hikari.ec.{BlockingECWrapper, ConnectECWrapper, TransactECWrapper}
import io.dlinov.auth.domain.algebras.{
  AuthenticationAlgebra,
  BackOfficeUserAlgebra,
  BusinessUnitAlgebra,
  DocumentAlgebra,
  PasswordAlgebra,
  PermissionAlgebra,
  RoleAlgebra,
  ScopeAlgebra
}
import io.dlinov.auth.domain.algebras.services.{
  CaptchaService,
  EmailNotificationService,
  NotificationService
}
import io.dlinov.auth.domain.auth.entities.BackOfficeUser
import io.dlinov.auth.routes.{
  AdminRoutes,
  AuthenticationRoutes,
  BackOfficeUserRoutes,
  BusinessUnitRoutes,
  HealthCheckRoutes,
  PermissionRoutes,
  RoleRoutes,
  ScopeRoutes,
  StaticsRoutes
}
import io.dlinov.auth.routes.proxied.{AccountsProxy, TransactionsProxy, TypesProxy}

import scala.concurrent.ExecutionContext

trait Bootable[F[_]] extends AuthenticationMiddlewareProvider[F] {
  implicit protected def ce: ConcurrentEffect[F]
  implicit protected def contextShift: ContextShift[F]
  implicit protected def timer: Timer[F]

  implicit override protected def syncF: Sync[F] = ce

  lazy val appConfig: AppConfig                       = AppConfig.load
  lazy val dbConfig: AppConfig.DbConfig               = appConfig.db
  lazy val authConfig: AppConfig.AuthConfig           = appConfig.auth
  lazy val loggerConfig: AppConfig.LogConfig          = appConfig.logging
  lazy val emailConfig: AppConfig.EmailConfig         = appConfig.email
  lazy val couchbaseConfig: AppConfig.CouchbaseConfig = appConfig.couchbase
  lazy val hdfsConfig: AppConfig.HdfsConfig           = appConfig.hdfs
  lazy val proxyConfig: AppConfig.ProxyConfig         = appConfig.proxy
  val connectEC = new ConnectECWrapper(
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  )
  val transactEC = new TransactECWrapper(
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  )
  // Static file support uses a blocking API, so we’ll need a blocking execution context:
  val webJarsEc = new BlockingECWrapper(
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
  )
  implicit val httpClientEC: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  lazy val httpClientResource: Resource[F, Client[F]] = BlazeClientBuilder[F](httpClientEC).resource

  lazy val dbApi: DBFApi[F]                         = wire[HikariDBFApi[F]]
  lazy val scopeDao: ScopeFDao[F]                   = wire[ScopeHikariDao[F]]
  lazy val roleDao: RoleFDao[F]                     = wire[RoleHikariDao[F]]
  lazy val buDao: BusinessUnitFDao[F]               = wire[BusinessUnitHikariDao[F]]
  lazy val permissionDao: PermissionHikariDao[F]    = wire[PermissionHikariDao[F]]
  lazy val backOfficeUserDao: BackOfficeUserFDao[F] = wire[BackOfficeUserHikariDao[F]]
  lazy val docDao: DocumentFDao[F]                  = wire[DocumentHikariDao[F]]
  lazy val blobHdfsDao: BlobFDao[F]                 = wire[BlobHdfsDao[F]]
  lazy val blobCouchbaseDao: BlobTmpFDao[F]         = wire[BlobCouchbaseDao[F]]

  lazy val captchaService: CaptchaService[F]               = wire[CaptchaService[F]]
  lazy val notificationService: NotificationService[F]     = wire[EmailNotificationService[F]]
  lazy val passwordAlgebra: PasswordAlgebra                = wire[PasswordAlgebra]
  lazy val businessUnitAlgebra: BusinessUnitAlgebra[F]     = wire[BusinessUnitAlgebra[F]]
  lazy val roleAlgebra: RoleAlgebra[F]                     = wire[RoleAlgebra[F]]
  lazy val scopeService: ScopeAlgebra[F]                   = wire[ScopeAlgebra[F]]
  lazy val backOfficeUserAlgebra: BackOfficeUserAlgebra[F] = wire[BackOfficeUserAlgebra[F]]
  lazy val permissionAlgebra: PermissionAlgebra[F]         = wire[PermissionAlgebra[F]]
  lazy val authenticationAlgebra: AuthenticationAlgebra[F] = wire[AuthenticationAlgebra[F]]
  lazy val documentAlgebra: DocumentAlgebra[F]             = wire[DocumentAlgebra[F]]

  val auth = new AuthedContext[F, BackOfficeUser]

  private lazy val healthCheckRoutes    = wire[HealthCheckRoutes[F]]
  private lazy val adminRoutes          = wire[AdminRoutes[F]]
  private lazy val authenticationRoutes = wire[AuthenticationRoutes[F]]
  private lazy val bouRoutes            = wire[BackOfficeUserRoutes[F]]
  private lazy val buRoutes             = wire[BusinessUnitRoutes[F]]
  private lazy val roleRoutes           = wire[RoleRoutes[F]]
  private lazy val permissionRoutes     = wire[PermissionRoutes[F]]
  private lazy val scopesRoutes         = wire[ScopeRoutes[F]]
  private lazy val accountsProxy        = wire[AccountsProxy[F]]
  private lazy val txnsProxy            = wire[TransactionsProxy[F]]
  private lazy val typesProxy           = wire[TypesProxy[F]]
  private lazy val staticsRoutes        = wire[StaticsRoutes[F]]

  private val apiContentTypes = List("application/json")
  private val swaggerAnonMiddleware = SwaggerSupport[F].createRhoMiddleware(
    swaggerFormats = org.http4s.rho.swagger.DefaultSwaggerFormats, // SwaggerFormats,
    apiPath = TypedPath(PathMatch(AuthenticationMiddlewareProvider.SwaggerAnonPath)),
    apiInfo = Info(title = "Authentication API (ANONYMOUS PART)", version = BuildInfo.version),
    consumes = apiContentTypes,
    produces = apiContentTypes
  )
  private val swaggerMiddleware = SwaggerSupport[F].createRhoMiddleware(
    apiInfo = Info(title = "Authentication API", version = BuildInfo.version),
    consumes = apiContentTypes,
    produces = apiContentTypes,
    security = List(SecurityRequirement("Bearer", List.empty[String])),
    securityDefinitions = Map("Bearer" → ApiKeyAuthDefinition("Authorization", In.HEADER))
  )

  val services: HttpApp[F] = {
    val anonymousRoutes = NonEmptyList
      .of(authenticationRoutes, healthCheckRoutes, typesProxy)
      .map(_.routes)
      .reduceLeft(_ and _)
      .toRoutes(swaggerAnonMiddleware)
    val authenticatedRoutes = Authenticated(
      auth.toService(
        NonEmptyList
          .of(
            scopesRoutes,
            bouRoutes,
            buRoutes,
            roleRoutes,
            permissionRoutes,
            accountsProxy,
            txnsProxy,
            adminRoutes
          )
          .map(_.routes)
          .reduceLeft(_ and _)
          .toRoutes(swaggerMiddleware)
      )
    )
    val logAction: String ⇒ F[Unit] = {
      if (loggerConfig.isEnabled) {
        val logger = LoggerFactory.getLogger("request-response")
        msg ⇒ ce.delay(logger.debug(msg))
      } else { _ ⇒
        ce.unit
      }
    }
    val loggedRoutes = Logger.httpRoutes(
      logHeaders = loggerConfig.logHeaders,
      logBody = loggerConfig.logBody,
      logAction = Some(logAction)
    )(anonymousRoutes <+> authenticatedRoutes)
    val routesConcatenated: HttpRoutes[F] = staticsRoutes.routes <+> loggedRoutes
    GZip(routesConcatenated).orNotFound
  }
}
