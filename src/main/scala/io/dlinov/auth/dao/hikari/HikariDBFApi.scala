package io.dlinov.auth.dao.hikari

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.UUID

import cats.effect.{ContextShift, IO}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie._
import doobie.syntax.string._
import doobie.hikari.HikariTransactor
import doobie.util.log.{ExecFailure, LogHandler, ProcessingFailure, Success}
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.hikari.ec.{ConnectECWrapper, TransactECWrapper}
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.routes.dto.PermissionKey
import org.slf4j.LoggerFactory
import io.dlinov.auth.domain.auth.entities.Email
import io.dlinov.auth.AppConfig.DbConfig
import io.dlinov.auth.dao.DBFApi
import io.dlinov.auth.dao.hikari.ec.{ConnectECWrapper, TransactECWrapper}
import io.dlinov.auth.routes.dto.PermissionKey
import io.dlinov.auth.routes.dto.PermissionKeys.{BusinessUnitAndRolePermissionKey, UserPermissionKey}

class HikariDBFApi(
    dbConfig: DbConfig,
    connectEC: ConnectECWrapper,
    transactEC: TransactECWrapper)(implicit val cs: ContextShift[IO]) extends DBFApi[IO] {

  val config = new HikariConfig()
  config.setJdbcUrl(dbConfig.url)
  config.setUsername(dbConfig.user)
  config.setPassword(dbConfig.password)
  config.setMinimumIdle(dbConfig.minIdle)
  config.setMaximumPoolSize(dbConfig.poolSize)

  println(config.getJdbcUrl())
  private val hikariDataSource = new HikariDataSource(config)
  private val hikariTransactor: HikariTransactor[IO] =
    HikariTransactor[IO](hikariDataSource, connectEC.ec, transactEC.ec)

  override val transactor: IO[HikariTransactor[IO]] = IO.pure(hikariTransactor)
}

object HikariDBFApi {

  implicit val lh: LogHandler = {
    val logger = LoggerFactory.getLogger("sql")
    LogHandler {

      case Success(s, a, e1, e2) ⇒
        logger.debug(s"""Successful Statement Execution:
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            | arguments = [${a.mkString(", ")}]
            | elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
          """.stripMargin)

      case ProcessingFailure(s, a, e1, e2, t) ⇒
        logger.warn(s"""Failed Resultset Processing:
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            | arguments = [${a.mkString(", ")}]
            | elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
            | failure = ${t.getMessage}
          """.stripMargin)

      case ExecFailure(s, a, e1, t) ⇒
        logger.error(s"""Failed Statement Execution:
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            | arguments = [${a.mkString(", ")}]
            | elapsed = ${e1.toMillis} ms exec (failed)
            | failure = ${t.getMessage}
          """.stripMargin)

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

  implicit val pKeyRead: Read[PermissionKey] = Read[(Option[UUID], Option[UUID], Option[UUID])].map {
    case (_, Some(userId), _) ⇒ UserPermissionKey(userId)
    case (Some(buId), _, Some(roleId)) ⇒ BusinessUnitAndRolePermissionKey(buId, roleId)
  }

  implicit val pKeyWrite: Write[PermissionKey] = Write[(Option[UUID], Option[UUID], Option[UUID])].contramap {
    case BusinessUnitAndRolePermissionKey(buId, roleId) ⇒ (Some(buId), None, Some(roleId))
    case UserPermissionKey(userId) ⇒ (None, Some(userId), None)
  }
}
