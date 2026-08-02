package io.dlinov.auth.dao.hikari

import java.util.UUID

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.dlinov.auth.AppConfig.DbConfig
import io.dlinov.auth.dao.hikari.ec.ConnectECWrapper
import io.dlinov.auth.domain.PaginatedResult
import io.dlinov.auth.domain.auth.entities.{BusinessUnit, Email, Role, Scope}
import io.dlinov.auth.routes.dto.PermissionKeys.{
  BusinessUnitAndRolePermissionKey,
  UserPermissionKey
}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Random

/** Focused DAO-level tests for the single-SQL merge that replaced
  * `PermissionHikariDao.findAndMergeInternal`'s previous two-unbounded-query-plus-in-memory-merge
  * implementation, and for the batched (N+1-free) permission attachment used by
  * `BackOfficeUserHikariDao.findAllInternal`. No DAO-level tests existed previously; HTTP-route
  * level tests (`PermissionRoutesSpec`, `BackOfficeUserRoutesSpec`) exercise the same code
  * indirectly, but not the negative-permission-override edge cases below.
  */
class PermissionHikariDaoSpec extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  private val dbSuffix = Random.nextInt(Short.MaxValue)
  private val dbConfig = DbConfig(
    url = s"jdbc:h2:mem:permission_dao_spec_$dbSuffix;" +
      "MODE=MYSQL;DATABASE_TO_UPPER=FALSE;IGNORECASE=TRUE;INIT=RUNSCRIPT FROM 'classpath:init.sql'",
    user = "",
    password = "",
    minIdle = 1,
    poolSize = 5
  )

  private var release: IO[Unit]                              = IO.unit
  private var dbApi: HikariDBFApi[IO]                        = scala.compiletime.uninitialized
  private var scopeDao: ScopeHikariDao[IO]                   = scala.compiletime.uninitialized
  private var buDao: BusinessUnitHikariDao[IO]               = scala.compiletime.uninitialized
  private var roleDao: RoleHikariDao[IO]                     = scala.compiletime.uninitialized
  private var permissionDao: PermissionHikariDao[IO]         = scala.compiletime.uninitialized
  private var backOfficeUserDao: BackOfficeUserHikariDao[IO] = scala.compiletime.uninitialized

  override def beforeAll(): Unit = {
    super.beforeAll()
    val resources = for {
      ds <- HikariDBFApi.dataSourceResource[IO](dbConfig)
      ec <- ConnectECWrapper.resource[IO](dbConfig.poolSize)
    } yield (ds, ec)
    val ((ds, ec), rel) = resources.allocated.unsafeRunSync()
    release = rel
    dbApi = new HikariDBFApi[IO](ds, ec)
    scopeDao = new ScopeHikariDao[IO](dbApi)
    buDao = new BusinessUnitHikariDao[IO](dbApi)
    roleDao = new RoleHikariDao[IO](dbApi)
    permissionDao = new PermissionHikariDao[IO](dbApi)
    backOfficeUserDao = new BackOfficeUserHikariDao[IO](dbApi, permissionDao)
  }

  override def afterAll(): Unit = {
    release.unsafeRunSync()
    super.afterAll()
  }

  private def createScope(name: String): Scope =
    scopeDao.create(name, None, None, "system", reactivate = false).unsafeRunSync().toOption.get

  private def createBu(name: String): BusinessUnit =
    buDao.create(name, "system", reactivate = false).unsafeRunSync().toOption.get

  private def createRole(name: String): Role =
    roleDao.create(name, "system", reactivate = false).unsafeRunSync().toOption.get

  private def grantBuRole(
      buId: UUID,
      roleId: UUID,
      scopeId: UUID,
      revoke: Boolean = false
  ): Unit = {
    permissionDao
      .create(BusinessUnitAndRolePermissionKey(buId, roleId), scopeId, revoke, "system", false)
      .unsafeRunSync()
      .toOption
      .get
    ()
  }

  private def grantUser(userId: UUID, scopeId: UUID, revoke: Boolean = false): Unit = {
    permissionDao
      .create(UserPermissionKey(userId), scopeId, revoke, "system", false)
      .unsafeRunSync()
      .toOption
      .get
    ()
  }

  private def createUser(name: String, roleId: UUID, buId: UUID): UUID =
    backOfficeUserDao
      .create(
        userName = name,
        password = "password",
        email = Email(s"$name@foo.bar"),
        phoneNumber = None,
        firstName = name,
        middleName = None,
        lastName = name,
        description = None,
        homePage = None,
        activeLanguage = None,
        customData = None,
        roleId = roleId,
        businessUnitId = buId,
        createdBy = "system",
        reactivate = false
      )
      .unsafeRunSync()
      .toOption
      .get
      .id

  "PermissionHikariDao.findAndMerge (management view, no user)" should {
    "keep negative/revoked business-unit/role permissions" in {
      val bu    = createBu("bu-mgmt")
      val role  = createRole("role-mgmt")
      val scope = createScope("scope-mgmt-negative")
      grantBuRole(bu.id, role.id, scope.id, revoke = true)

      val result =
        permissionDao.findAndMerge(bu.id, role.id, None, None, None).unsafeRunSync().toOption.get
      result.results.map(_.scope.name) must contain(scope.name)
      result.results.find(_.scope.name == scope.name).map(_.status) mustBe Some(-1)
    }

    "paginate accurately (limit/offset and total)" in {
      val bu     = createBu("bu-mgmt-page")
      val role   = createRole("role-mgmt-page")
      val scopes = (1 to 5).map(i => createScope(s"scope-mgmt-page-$i"))
      scopes.foreach(s => grantBuRole(bu.id, role.id, s.id))

      val all: PaginatedResult[?] =
        permissionDao.findAndMerge(bu.id, role.id, None, None, None).unsafeRunSync().toOption.get
      all.total mustBe 5
      all.results.size mustBe 5

      val page =
        permissionDao
          .findAndMerge(bu.id, role.id, None, Some(2), Some(2))
          .unsafeRunSync()
          .toOption
          .get
      page.total mustBe 5
      page.results.size mustBe 2
    }
  }

  "PermissionHikariDao.findAndMerge (effective view, with user)" should {
    "let a user-specific negative permission override and remove a positive BU/role permission" in {
      val bu     = createBu("bu-eff-1")
      val role   = createRole("role-eff-1")
      val scope  = createScope("scope-eff-override")
      val userId = createUser("user-eff-1", role.id, bu.id)
      grantBuRole(bu.id, role.id, scope.id, revoke = false)
      grantUser(userId, scope.id, revoke = true)

      val result = permissionDao
        .findAndMerge(bu.id, role.id, Some(userId), None, None)
        .unsafeRunSync()
        .toOption
        .get
      result.results.map(_.scope.name) must not contain scope.name
    }

    "include a BU/role permission with no user override, and a user-only permission" in {
      val bu        = createBu("bu-eff-2")
      val role      = createRole("role-eff-2")
      val buScope   = createScope("scope-eff-bu-only")
      val userScope = createScope("scope-eff-user-only")
      val userId    = createUser("user-eff-2", role.id, bu.id)
      grantBuRole(bu.id, role.id, buScope.id)
      grantUser(userId, userScope.id)

      val result = permissionDao
        .findAndMerge(bu.id, role.id, Some(userId), None, None)
        .unsafeRunSync()
        .toOption
        .get
      result.results.map(_.scope.name) must contain allOf (buScope.name, userScope.name)
    }

    "not leak another user's overrides onto this user's effective permissions" in {
      val bu    = createBu("bu-eff-3")
      val role  = createRole("role-eff-3")
      val scope = createScope("scope-eff-other-user")
      val userA = createUser("user-eff-3a", role.id, bu.id)
      val userB = createUser("user-eff-3b", role.id, bu.id)
      grantUser(userA, scope.id)

      val resultB = permissionDao
        .findAndMerge(bu.id, role.id, Some(userB), None, None)
        .unsafeRunSync()
        .toOption
        .get
      resultB.results.map(_.scope.name) must not contain scope.name
    }
  }

  "BackOfficeUserHikariDao.findAll" should {
    "attach the correct effective permissions per user on a page, without cross-contamination" in {
      val bu     = createBu("bu-page")
      val role   = createRole("role-page")
      val shared = createScope("scope-page-shared")
      val onlyA  = createScope("scope-page-only-a")
      grantBuRole(bu.id, role.id, shared.id)

      val userAId = createUser("page-user-a", role.id, bu.id)
      val userBId = createUser("page-user-b", role.id, bu.id)
      grantUser(userAId, onlyA.id)

      val page = backOfficeUserDao
        .findAll(None, None, None, None, None, None)
        .unsafeRunSync()
        .toOption
        .get
      val byId = page.results.map(u => u.id -> u).toMap

      byId(userAId).permissions.map(_.scope.name) must contain allOf (shared.name, onlyA.name)
      byId(userBId).permissions.map(_.scope.name) must contain(shared.name)
      byId(userBId).permissions.map(_.scope.name) must not contain onlyA.name
    }
  }
}
