package io.dlinov.auth

import cats.effect.Async
import cats.syntax.semigroupk._
import com.zaxxer.hikari.HikariDataSource
import fs2.compression.Compression
import org.http4s.client.Client
import com.softwaremill.macwire._
import org.http4s.{AuthedRoutes, HttpApp, HttpRoutes}
import org.http4s.implicits._
import org.http4s.server.middleware._
import org.slf4j.LoggerFactory
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.generic.{BackOfficeUserFDao, BusinessUnitFDao, RoleFDao, ScopeFDao}
import io.dlinov.auth.dao.hikari.{
  BackOfficeUserHikariDao,
  BusinessUnitHikariDao,
  HikariDBFApi,
  PermissionHikariDao,
  RoleHikariDao,
  ScopeHikariDao
}
import io.dlinov.auth.dao.hikari.ec.ConnectECWrapper
import io.dlinov.auth.domain.algebras.{
  AuthenticationAlgebra,
  BackOfficeUserAlgebra,
  BusinessUnitAlgebra,
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
  ScopeRoutes
}
import io.dlinov.auth.routes.docs.DocsRoutes
import io.dlinov.auth.routes.proxied.{AccountsProxy, TransactionsProxy, TypesProxy}

trait Bootable[F[_]] extends AuthenticationMiddlewareProvider[F] {
  implicit override protected def asyncF: Async[F]
  implicit private lazy val compression: Compression[F] = Compression.forSync[F](using asyncF)

  lazy val appConfig: AppConfig                 = AppConfig.load
  lazy val dbConfig: AppConfig.DbConfig         = appConfig.db
  lazy val authConfig: AppConfig.AuthConfig     = appConfig.auth
  lazy val loggerConfig: AppConfig.LogConfig    = appConfig.logging
  lazy val emailConfig: AppConfig.EmailConfig   = appConfig.email
  lazy val proxyConfig: AppConfig.ProxyConfig   = appConfig.proxy
  lazy val serverConfig: AppConfig.ServerConfig = appConfig.server

  // Acquired/released for the whole application (or test-suite) lifetime by the concrete
  // entry point (CatsIOBoot) or test scaffold (Http4sSpec) via cats.effect.Resource: the
  // HikariCP pool, its bounded connection-acquisition executor and the Ember HTTP client used
  // to be self-constructed `lazy val`s here, which meant e.g. a new Ember client (and
  // connection pool) was built and torn down on every single proxied/captcha request. Now they
  // are constructed exactly once and simply supplied to this trait.
  protected def hikariDataSource: HikariDataSource
  protected def connectEC: ConnectECWrapper
  protected def httpClient: Client[F]

  lazy val dbApi: DBFApi[F]                         = wire[HikariDBFApi[F]]
  lazy val scopeDao: ScopeFDao[F]                   = wire[ScopeHikariDao[F]]
  lazy val roleDao: RoleFDao[F]                     = wire[RoleHikariDao[F]]
  lazy val buDao: BusinessUnitFDao[F]               = wire[BusinessUnitHikariDao[F]]
  lazy val permissionDao: PermissionHikariDao[F]    = wire[PermissionHikariDao[F]]
  lazy val backOfficeUserDao: BackOfficeUserFDao[F] = wire[BackOfficeUserHikariDao[F]]

  lazy val captchaService: CaptchaService[F]               = wire[CaptchaService[F]]
  lazy val notificationService: NotificationService[F]     = wire[EmailNotificationService[F]]
  lazy val passwordAlgebra: PasswordAlgebra                = wire[PasswordAlgebra]
  lazy val businessUnitAlgebra: BusinessUnitAlgebra[F]     = wire[BusinessUnitAlgebra[F]]
  lazy val roleAlgebra: RoleAlgebra[F]                     = wire[RoleAlgebra[F]]
  lazy val scopeService: ScopeAlgebra[F]                   = wire[ScopeAlgebra[F]]
  lazy val backOfficeUserAlgebra: BackOfficeUserAlgebra[F] = wire[BackOfficeUserAlgebra[F]]
  lazy val permissionAlgebra: PermissionAlgebra[F]         = wire[PermissionAlgebra[F]]
  lazy val authenticationAlgebra: AuthenticationAlgebra[F] = wire[AuthenticationAlgebra[F]]

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
  private lazy val docsRoutes           = wire[DocsRoutes[F]]

  // `lazy`: forcing this eagerly would force the whole DI graph (including `dbApi`, which reads
  // `hikariDataSource`/`connectEC`) at object-construction time - before `CatsIOBoot`/
  // `Http4sSpec` have actually acquired and injected those resources.
  lazy val services: HttpApp[F] = {
    val anonymousRoutes: HttpRoutes[F] =
      List(authenticationRoutes.routes, healthCheckRoutes.routes, docsRoutes.routes)
        .reduceLeft(_ <+> _)
    val authenticatedRoutesRaw: AuthedRoutes[BackOfficeUser, F] =
      List(
        scopesRoutes.routes,
        bouRoutes.routes,
        buRoutes.routes,
        roleRoutes.routes,
        permissionRoutes.routes,
        accountsProxy.routes,
        txnsProxy.routes,
        typesProxy.routes,
        adminRoutes.routes
      ).reduceLeft(_ <+> _)
    val authenticatedRoutes: HttpRoutes[F] =
      ErrorHandling.httpRoutes(Authenticated(authenticatedRoutesRaw))
    val logAction: String => F[Unit] = {
      if (loggerConfig.isEnabled) {
        val logger = LoggerFactory.getLogger("request-response")
        msg => asyncF.delay(logger.debug(msg))
      } else { _ =>
        asyncF.unit
      }
    }
    val loggedRoutes = Logger.httpRoutes(
      logHeaders = loggerConfig.logHeaders,
      logBody = loggerConfig.logBody,
      logAction = Some(logAction)
    )(ErrorHandling.httpRoutes(anonymousRoutes) <+> authenticatedRoutes)
    GZip(loggedRoutes).orNotFound
  }
}
