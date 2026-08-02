enablePlugins(DockerPlugin, JavaAppPackaging)

name := "auth-api"
version := "1.1.0"

Test / javaOptions += "-Dconfig.file=src/test/resources/application.test.conf"
Test / fork := true

lazy val auth_api = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "io.dlinov.auth"
  )

scalaVersion := "3.8.4"

val catsVersion       = "2.13.0"
val catsEffectVersion = "3.7.0"
val circeVersion      = "0.14.16"
val doobieVersion     = "1.0.0-RC12"
val http4sVersion     = "0.23.36"
val monocleVersion    = "3.3.0"
val pureConfigVersion = "0.17.10"
val jwtVersion        = "11.0.4"
val macwireVersion    = "2.6.7"
val caffeineVersion   = "3.2.4"
val logbackVersion    = "1.6.1"
val mariaDbVersion    = "3.5.10"
val tapirVersion      = "1.13.30"
val commonsCodecVersion = "1.22.1"
val bcVersion         = "1.85"
val commonsEmailVersion = "1.6.0"
val commonsTextVersion  = "1.15.0"
val scalatestVersion    = "3.2.20"

val h2Version = "2.4.240"

libraryDependencies ++= Seq(
  "org.mariadb.jdbc"               % "mariadb-java-client"    % mariaDbVersion,
  "commons-codec"                  % "commons-codec"          % commonsCodecVersion,
  "org.apache.commons"             % "commons-email"          % commonsEmailVersion,
  "org.apache.commons"             % "commons-text"           % commonsTextVersion,
  "org.typelevel"                 %% "cats-core"              % catsVersion,
  "org.typelevel"                 %% "cats-effect"            % catsEffectVersion,
  "com.github.ben-manes.caffeine"  % "caffeine"               % caffeineVersion,
  "com.github.pureconfig"         %% "pureconfig-core"        % pureConfigVersion,
  "com.github.pureconfig"         %% "pureconfig-generic-scala3" % pureConfigVersion,
  "com.github.pureconfig"         %% "pureconfig-cats-effect" % pureConfigVersion,
  "com.github.pureconfig"         %% "pureconfig-http4s"      % pureConfigVersion,
  "io.circe"                      %% "circe-core"             % circeVersion,
  "io.circe"                      %% "circe-parser"           % circeVersion,
  "com.github.jwt-scala"          %% "jwt-circe"              % jwtVersion,
  "org.http4s"                    %% "http4s-circe"           % http4sVersion,
  "org.http4s"                    %% "http4s-dsl"             % http4sVersion,
  "org.http4s"                    %% "http4s-ember-server"    % http4sVersion,
  "org.http4s"                    %% "http4s-ember-client"    % http4sVersion,
  "org.tpolecat"                  %% "doobie-core"            % doobieVersion,
  "org.tpolecat"                  %% "doobie-hikari"          % doobieVersion,
  "dev.optics"                    %% "monocle-core"           % monocleVersion,
  "ch.qos.logback"                 % "logback-classic"        % logbackVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server"    % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"       % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle" % tapirVersion,
  "org.bouncycastle"               % "bcprov-jdk18on"         % bcVersion,
  "com.softwaremill.macwire"      %% "macros"                 % macwireVersion % Provided,
  "org.scalatest"                 %% "scalatest"              % scalatestVersion % Test,
  "com.h2database"                 % "h2"                     % h2Version % Test,
  "org.tpolecat"                  %% "doobie-h2"              % doobieVersion % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explain",
  "-feature",
  "-unchecked",
  "-Wvalue-discard",
  "-Wunused:all",
  "-Xmax-inlines",
  "64"
)

dockerBaseImage := "eclipse-temurin:26-jre-alpine"
dockerExposedPorts := Seq(9000)
Docker / daemonUser := "nobody"
Docker / daemonGroup := "nogroup"
Docker / packageName := "auth-api"

coverageMinimumStmtTotal := 86
coverageFailOnMinimum := false
