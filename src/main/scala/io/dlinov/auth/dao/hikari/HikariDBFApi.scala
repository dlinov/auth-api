package io.dlinov.auth.dao.hikari

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.effect.{Async, Resource, Sync}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie._
import doobie.implicits.javatimedrivernative.JavaInstantMeta
import doobie.syntax.string._
import doobie.hikari.HikariTransactor
import doobie.util.log.{ExecFailure, LogEvent, LogHandler, ProcessingFailure, Success}
import io.dlinov.auth.AppConfig.DbConfig
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.hikari.ec.ConnectECWrapper
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.dto.PermissionKey
import io.dlinov.auth.routes.dto.PermissionKeys.{
  BusinessUnitAndRolePermissionKey,
  UserPermissionKey
}
import org.slf4j.LoggerFactory

class HikariDBFApi[F[_]: Async](
    dataSource: HikariDataSource,
    connectEC: ConnectECWrapper
) extends DBFApi[F] {

  // doobie 1.0 dropped cats-effect 2's Blocker/ContextShift: blocking JDBC calls now run via
  // Async[F]'s own blocking support, so only a single (connection-acquisition) EC is needed.
  // The HikariDataSource itself is built and its lifecycle managed by the caller (see
  // Bootable/CatsIOBoot/Http4sSpec) via cats.effect.Resource, so it is created and closed
  // exactly once for the application's/test suite's lifetime rather than per-DAO-instance.
  private val hikariTransactor: HikariTransactor[F] =
    HikariTransactor[F](dataSource, connectEC.ec, Some(HikariDBFApi.logHandler[F]))

  override val transactor: F[Transactor[F]] = {
    Async[F].delay[Transactor[F]](hikariTransactor)
  }
}

object HikariDBFApi {

  private val sqlLogger = LoggerFactory.getLogger("sql")

  /** Builds the single `HikariDataSource` backing the application (or a test suite) as a
    * `Resource`, so it is guaranteed to be closed (and its underlying connection pool released)
    * exactly once, on application shutdown / test suite `afterAll`, rather than never being closed
    * at all as previously (`new HikariDataSource(dbConfig)` was constructed with no corresponding
    * `.close()` call anywhere).
    */
  def dataSourceResource[F[_]: Sync](dbConfig: DbConfig): Resource[F, HikariDataSource] = {
    Resource.make(Sync[F].delay {
      val config = new HikariConfig()
      config.setJdbcUrl(dbConfig.url)
      config.setUsername(dbConfig.user)
      config.setPassword(dbConfig.password)
      config.setMinimumIdle(dbConfig.minIdle)
      config.setMaximumPoolSize(dbConfig.poolSize)
      new HikariDataSource(config)
    })(ds => Sync[F].delay(ds.close()))
  }

  def logHandler[F[_]: Sync]: LogHandler[F] = new LogHandler[F] {
    override def run(logEvent: LogEvent): F[Unit] = Sync[F].delay {
      logEvent match {
        // TODO: get rid of .toArray calls
        case Success(s, a, _, e1, e2) =>
          sqlLogger.debug(s"""Successful Statement Execution:
              |  ${s.linesIterator.dropWhile(_.trim.isEmpty).toArray.mkString("\n  ")}
              | arguments = [${a.allParams.mkString(", ")}]
              | elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
            """.stripMargin)

        case ProcessingFailure(s, a, _, e1, e2, t) =>
          sqlLogger.warn(s"""Failed Resultset Processing:
              |  ${s.linesIterator.dropWhile(_.trim.isEmpty).toArray.mkString("\n  ")}
              | arguments = [${a.allParams.mkString(", ")}]
              | elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
              | failure = ${t.getMessage}
            """.stripMargin)

        case ExecFailure(s, a, _, e1, t) =>
          sqlLogger.error(s"""Failed Statement Execution:
              |  ${s.linesIterator.dropWhile(_.trim.isEmpty).toArray.mkString("\n  ")}
              | arguments = [${a.allParams.mkString(", ")}]
              | elapsed = ${e1.toMillis} ms exec (failed)
              | failure = ${t.getMessage}
            """.stripMargin)
      }
    }
  }

  val EmptyFragment = fr""

  implicit val ZonedDateTimeMeta: Meta[ZonedDateTime] = {
    val utc = ZoneOffset.UTC
    Meta[Instant].timap(ZonedDateTime.ofInstant(_, utc))(Instant.from)
  }

  implicit val UuidMeta: Meta[UUID] = {
    Meta[String].timap(UUID.fromString)(_.toString)
  }

  implicit val EmailMeta: Meta[Email] = {
    Meta[String].timap(Email.apply)(_.value)
  }

  implicit val pKeyRead: Read[PermissionKey] =
    Read[(Option[UUID], Option[UUID], Option[UUID])].map {
      case (_, Some(userId), _)          => UserPermissionKey(userId)
      case (Some(buId), _, Some(roleId)) => BusinessUnitAndRolePermissionKey(buId, roleId)
      case _                             =>
        throw new IllegalArgumentException(
          "Row should be either user or business unit/role permission"
        )
    }

  implicit val pKeyWrite: Write[PermissionKey] =
    Write[(Option[UUID], Option[UUID], Option[UUID])].contramap {
      case BusinessUnitAndRolePermissionKey(buId, roleId) => (Some(buId), None, Some(roleId))
      case UserPermissionKey(userId)                      => (None, Some(userId), None)
    }
}
