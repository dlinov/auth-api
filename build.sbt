enablePlugins(DockerPlugin, JavaAppPackaging)

name := "auth-api"
version := "1.1.0"

// to let integration tests use classes from test
val MyIntegrationTestConfig = IntegrationTest.extend(Test)

Test / javaOptions += "-Dconfig.file=src/test/resources/application.test.conf"
Test / fork := true

MyIntegrationTestConfig / javaOptions += "-Dconfig.file=src/it/resources/application.it.conf"
MyIntegrationTestConfig / fork := true
// because we share single db and clean it before each test
MyIntegrationTestConfig / parallelExecution := false

lazy val auth_api = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .configs(MyIntegrationTestConfig)
  .settings(
    Defaults.itSettings,
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      BuildInfoKey.constant("swaggerVersion", swaggerUiVersion)
    ),
    buildInfoPackage := "io.dlinov.auth"
  )

scalaVersion := "2.12.10"

val catsVersion         = "2.0.0"
val circeVersion        = "0.12.2"
val doobieVersion       = "0.8.4"
val http4sVersion       = "0.21.0-M5"
val http4sTracerVersion = "1.2.1"
val rhoVersion          = "0.19.0"
val monocleVersion      = "2.0.0"
val pureConfigVersion   = "0.12.1"
val scalaCacheVersion   = "0.28.0"
val jwtVersion          = "4.1.0"
val macwireVersion      = "2.3.3"
val scalaExtVersion     = "0.5.3"
val logbackVersion      = "1.2.3"
val mariaDbVersion      = "2.5.1"
val couchbaseVersion    = "2.7.9"
val hadoopVersion       = "3.2.1"
val bcVersion           = "1.64"
val commonsEmailVersion = "1.5"
val commonsTextVersion  = "1.8"
val swaggerUiVersion    = "3.24.0"
val scalatestVersion    = "3.0.8"
val monixVersion        = "3.0.0"

val h2Version = "1.4.200"

val AllTest = "it,test"
libraryDependencies ++= Seq(
  "org.mariadb.jdbc"           % "mariadb-java-client"     % mariaDbVersion,
  "org.apache.commons"         % "commons-email"           % commonsEmailVersion,
  "org.apache.commons"         % "commons-text"            % commonsTextVersion,
  "org.cvogt"                  %% "scala-extensions"       % scalaExtVersion,
  "org.typelevel"              %% "cats-core"              % catsVersion,
  "com.github.cb372"           %% "scalacache-caffeine"    % scalaCacheVersion,
  "com.github.cb372"           %% "scalacache-cats-effect" % scalaCacheVersion,
  "com.github.pureconfig"      %% "pureconfig"             % pureConfigVersion,
  "com.github.pureconfig"      %% "pureconfig-cats-effect" % pureConfigVersion,
  "com.github.pureconfig"      %% "pureconfig-http4s"      % pureConfigVersion,
  "io.circe"                   %% "circe-core"             % circeVersion,
  "io.circe"                   %% "circe-generic"          % circeVersion,
  "io.circe"                   %% "circe-generic-extras"   % circeVersion,
  "io.circe"                   %% "circe-parser"           % circeVersion,
  "com.pauldijou"              %% "jwt-circe"              % jwtVersion,
  "org.http4s"                 %% "rho-swagger"            % rhoVersion,
  "org.http4s"                 %% "http4s-circe"           % http4sVersion,
  "org.http4s"                 %% "http4s-dsl"             % http4sVersion,
  "org.http4s"                 %% "http4s-blaze-server"    % http4sVersion,
  "org.http4s"                 %% "http4s-blaze-client"    % http4sVersion,
  "org.tpolecat"               %% "doobie-core"            % doobieVersion,
  "org.tpolecat"               %% "doobie-hikari"          % doobieVersion,
  "com.github.julien-truffaut" %% "monocle-core"           % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro"          % monocleVersion,
  "ch.qos.logback"             % "logback-classic"         % logbackVersion,
  "com.couchbase.client"       % "java-client"             % couchbaseVersion,
  "org.apache.hadoop"          % "hadoop-client"           % hadoopVersion,
  "org.bouncycastle"           % "bcprov-jdk15on"          % bcVersion,
  "org.webjars"                % "swagger-ui"              % swaggerUiVersion,
  "io.monix"                   %% "monix"                  % monixVersion,
  "com.softwaremill.macwire"   %% "macros"                 % macwireVersion % Provided,
  "org.scalatest"              %% "scalatest"              % scalatestVersion % AllTest,
  "com.h2database"             % "h2"                      % h2Version % Test,
  "org.tpolecat"               %% "doobie-h2"              % doobieVersion % Test,
  "org.tpolecat"               %% "doobie-scalatest"       % doobieVersion % AllTest,
  "com.github.julien-truffaut" %% "monocle-law"            % monocleVersion % AllTest,
  // "com.github.gvolpe"       %% "http4s-tracer"          % http4sTracerVersion,
  // "io.monix"                   %% "monix-cats"             % monixVersion,
)

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Xcheckinit",
  "-Xfuture",
  "-Xlint",
  "-Xmacro-settings:materialize-derivations",
  "-Ypartial-unification",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override"
)

dockerBaseImage := "openjdk:11-jre-slim"
dockerExposedPorts := Seq(9000)
/*
import com.typesafe.sbt.packager.docker.Cmd
dockerCommands := {
  val dockerCmds = dockerCommands.value
  val setWorkingDir = dockerCmds.indexWhere(_.makeContent.startsWith("WORKDIR "))
  val (cmds1, cmds2) = dockerCmds.splitAt(setWorkingDir + 1)
  cmds1 ++ Seq(
    Cmd("RUN", "apt update && apt install -y ssh telnet tar curl wget")
  ) ++ cmds2
}
 */
Docker / daemonUser := "nobody"
Docker / daemonGroup := "nogroup"
Docker / packageName := "auth-api"

coverageMinimum := 86
coverageFailOnMinimum := false
