package io.dlinov.auth.routes.docs

import java.util.UUID

import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.routes.dto._
import io.dlinov.auth.routes.HealthCheckRoutes.HealthInfo
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import DocsCodecs._
import TapirSupport._

/** The single authoritative list of Tapir endpoints for this service, covering every live http4s
  * route (see `Bootable.services`). This is documentation-only metadata - a "gradual bridge" -
  * describing path/method/parameter/body shapes for OpenAPI/Swagger UI; the actual request handling
  * still goes through the existing, tested http4s route classes and the existing `Authenticated`
  * `AuthMiddleware`, which this does not replace or duplicate.
  */
object ApiEndpoints {
  private val api = "api"

  // ------------------------------------------------------------------------------------------
  // Health check (anonymous)
  // ------------------------------------------------------------------------------------------
  private val healthRedirect = endpoint.get
    .in("")
    .out(statusCode(sttp.model.StatusCode.TemporaryRedirect))
    .description("Redirects to /health")

  private val health = endpoint.get
    .in("health")
    .out(jsonBody[HealthInfo])
    .description("Basic liveness/build-info check")

  // ------------------------------------------------------------------------------------------
  // Authentication (anonymous)
  // ------------------------------------------------------------------------------------------
  private val login = endpoint.post
    .in(api / "login")
    .in(jsonBody[CredentialsToRead])
    .out(jsonBody[LoginResponse])
    .errorOut(errorOutput)
    .description("Authenticates a back office user, returning a bearer token")

  private val updatePassword = endpoint.put
    .in(api / "update_password")
    .in(jsonBody[CredentialsToUpdate])
    .out(jsonBody[LoginResponse])
    .errorOut(errorOutput)

  private val sendResetPasswordLink = endpoint.post
    .in(api / "reset_password")
    .in(jsonBody[ResetPasswordLinkRequest])
    .errorOut(errorOutput)
    .description("Triggers a password-reset e-mail/notification")

  private val validateResetPasswordToken = endpoint.get
    .in(api / "reset_password")
    .in(query[String]("token"))
    .errorOut(errorOutput)

  private val resetPassword = endpoint.put
    .in(api / "reset_password")
    .in(jsonBody[PasswordReset])
    .out(jsonBody[LoginResponse])
    .errorOut(errorOutput)

  private val loginStatus = endpoint.get
    .in(api / "status")
    .out(jsonBody[LoginStatusResponse])
    .errorOut(errorOutput)

  private val validateToken = endpoint.get
    .in(api / "validate_token")
    .securityIn(bearerAuth)
    .out(jsonBody[BackOfficeUserToRead])
    .errorOut(errorOutput)

  // ------------------------------------------------------------------------------------------
  // Admin (authenticated, full-access)
  // ------------------------------------------------------------------------------------------
  private val adminResetPassword = endpoint.post
    .in(api / "admin" / "reset_password")
    .securityIn(bearerAuth)
    .in(jsonBody[ResetPasswordLinkRequest])
    .errorOut(errorOutput)

  // ------------------------------------------------------------------------------------------
  // Generic CRUD-shaped resource: scopes / roles / business_units / back_office_users /
  // permissions all follow the same POST(create)/GET(list, paginated)/GET(byId)/PUT(byId)/
  // DELETE(byId) shape, so the repetitive mutation/lookup-by-id parts are factored out here.
  // The paginated "list" (`GET` on the bare resource root) endpoint is built separately (see
  // `findAllEndpoint`/`backOfficeUserFindAll` below) since `back_office_users` documents extra
  // `first_name`/`last_name`/`email`/`phone_number` filters that the others don't have.
  // ------------------------------------------------------------------------------------------
  private def crudMutationEndpoints[
      TCreate: io.circe.Encoder: io.circe.Decoder: sttp.tapir.Schema,
      TRead: io.circe.Encoder: io.circe.Decoder: sttp.tapir.Schema,
      TUpdate: io.circe.Encoder: io.circe.Decoder: sttp.tapir.Schema
  ](
      resourceRoot: String
  ): List[AnyEndpoint] = {
    val root   = api / resourceRoot
    val create = endpoint.post
      .in(root)
      .in(query[Option[Boolean]]("reactivate"))
      .securityIn(bearerAuth)
      .in(jsonBody[TCreate])
      .out(statusCode(sttp.model.StatusCode.Created).and(jsonBody[TRead]))
      .errorOut(errorOutput)
    val findById = endpoint.get
      .in(root / path[UUID]("id"))
      .securityIn(bearerAuth)
      .out(jsonBody[TRead])
      .errorOut(errorOutput)
    val update = endpoint.put
      .in(root / path[UUID]("id"))
      .securityIn(bearerAuth)
      .in(jsonBody[TUpdate])
      .out(jsonBody[TRead])
      .errorOut(errorOutput)
    val remove = endpoint.delete
      .in(root / path[UUID]("id"))
      .securityIn(bearerAuth)
      .out(jsonBody[UUID])
      .errorOut(errorOutput)
    List(create, findById, update, remove)
  }

  /** Plain paginated listing (`limit`/`offset` only) shared by resources with no extra filters. */
  private def findAllEndpoint[TRead: io.circe.Encoder: io.circe.Decoder: sttp.tapir.Schema](
      resourceRoot: String
  ): AnyEndpoint =
    endpoint.get
      .in(api / resourceRoot)
      .in(query[Option[Int]]("limit"))
      .in(query[Option[Int]]("offset"))
      .securityIn(bearerAuth)
      .out(jsonBody[PaginatedResult[TRead]])
      .errorOut(errorOutput)

  private val scopeEndpoints =
    crudMutationEndpoints[ScopeToCreate, ScopeToRead, ScopeToUpdate]("scopes") :+
      findAllEndpoint[ScopeToRead]("scopes")
  private val roleEndpoints =
    crudMutationEndpoints[RoleToCreate, RoleToRead, RoleToUpdate]("roles") :+
      findAllEndpoint[RoleToRead]("roles")
  private val businessUnitEndpoints =
    crudMutationEndpoints[BusinessUnitToCreate, BusinessUnitToRead, BusinessUnitToUpdate](
      "business_units"
    ) :+ findAllEndpoint[BusinessUnitToRead]("business_units")

  // Unlike the other resources' plain paginated listing, back_office_users' GET also accepts
  // first_name/last_name/email/phone_number optional filters (see BackOfficeUserRoutes' GET
  // handler) - documented here on the same single GET /api/back_office_users endpoint rather
  // than as a separate/duplicate endpoint definition.
  private val backOfficeUserFindAll = endpoint.get
    .in(api / "back_office_users")
    .in(query[Option[String]]("first_name"))
    .in(query[Option[String]]("last_name"))
    .in(query[Option[String]]("email"))
    .in(query[Option[String]]("phone_number"))
    .in(query[Option[Int]]("limit"))
    .in(query[Option[Int]]("offset"))
    .securityIn(bearerAuth)
    .out(jsonBody[PaginatedResult[BackOfficeUserToRead]])
    .errorOut(errorOutput)
  private val backOfficeUserEndpoints =
    crudMutationEndpoints[BackOfficeUserToCreate, BackOfficeUserToRead, BackOfficeUserToUpdate](
      "back_office_users"
    ) :+ backOfficeUserFindAll

  // permissions' GET is keyed by business_unit_id/role_id/user_id rather than a simple paged
  // listing, and it has no "find by id" endpoint, so it is not a good fit for `crudEndpoints`.
  private val permissionsRoot  = api / "permissions"
  private val createPermission = endpoint.post
    .in(permissionsRoot)
    .in(query[Option[Boolean]]("reactivate"))
    .securityIn(bearerAuth)
    .in(jsonBody[PermissionToCreate])
    .out(statusCode(sttp.model.StatusCode.Created).and(jsonBody[PermissionToRead]))
    .errorOut(errorOutput)
  private val findPermissions = endpoint.get
    .in(permissionsRoot)
    .in(query[UUID]("business_unit_id"))
    .in(query[UUID]("role_id"))
    .in(query[Option[UUID]]("user_id"))
    .in(query[Option[Int]]("limit"))
    .in(query[Option[Int]]("offset"))
    .securityIn(bearerAuth)
    .out(jsonBody[PaginatedResult[PermissionToRead]])
    .errorOut(errorOutput)
  private val updatePermission = endpoint.put
    .in(permissionsRoot / path[UUID]("id"))
    .securityIn(bearerAuth)
    .in(jsonBody[PermissionToUpdate])
    .out(jsonBody[PermissionToRead])
    .errorOut(errorOutput)
  private val removePermission = endpoint.delete
    .in(permissionsRoot / path[UUID]("id"))
    .securityIn(bearerAuth)
    .out(jsonBody[UUID])
    .errorOut(errorOutput)
  private val permissionEndpoints =
    List(createPermission, findPermissions, updatePermission, removePermission)

  // ------------------------------------------------------------------------------------------
  // Reverse proxies: forward arbitrary, downstream-defined JSON payloads verbatim, so their
  // bodies are intentionally left untyped here (see `ProxySupport`/`ProxiedRoute`/`TypesProxy`
  // for the actual streaming/forwarding behavior); only the authenticated path/method surface
  // - which IS fully specified by this service - is documented.
  // ------------------------------------------------------------------------------------------
  private def proxyEndpoints(resourceRoot: String): List[AnyEndpoint] = {
    val root = api / resourceRoot / paths
    List(
      endpoint.get.in(root).securityIn(bearerAuth).errorOut(errorOutput),
      endpoint.post.in(root).securityIn(bearerAuth).errorOut(errorOutput),
      endpoint.put.in(root).securityIn(bearerAuth).errorOut(errorOutput),
      endpoint.delete.in(root).securityIn(bearerAuth).errorOut(errorOutput)
    )
  }

  private val accountsProxyEndpoints     = proxyEndpoints("accounts")
  private val transactionsProxyEndpoints = proxyEndpoints("transactions")

  private def readOnlyProxyEndpoint(resourceRoot: String): AnyEndpoint =
    endpoint.get.in(api / resourceRoot / paths).securityIn(bearerAuth).errorOut(errorOutput)

  private val typesProxyEndpoints = List(
    readOnlyProxyEndpoint("types"),
    readOnlyProxyEndpoint("currencies"),
    readOnlyProxyEndpoint("account_types")
  )

  val all: List[AnyEndpoint] =
    List(
      healthRedirect,
      health,
      login,
      updatePassword,
      sendResetPasswordLink,
      validateResetPasswordToken,
      resetPassword,
      loginStatus,
      validateToken,
      adminResetPassword
    ) ++ scopeEndpoints ++ roleEndpoints ++ businessUnitEndpoints ++
      backOfficeUserEndpoints ++ permissionEndpoints ++
      accountsProxyEndpoints ++ transactionsProxyEndpoints ++ typesProxyEndpoints
}
