import com.typesafe.sbt.packager.docker.Cmd
import scalariform.formatter.preferences._

enablePlugins(DockerPlugin, JavaAppPackaging)

name := "auth-api"
version := "1.0.25"

// to let integration tests use classes from test
val MyIntegrationTestConfig = IntegrationTest.extend(Test)

Test / javaOptions += "-Dconfig.file=src/test/resources/application.test.conf"
Test / fork := true

MyIntegrationTestConfig / javaOptions += "-Dconfig.file=src/it/resources/application.it.conf"
MyIntegrationTestConfig / fork := true
// because we share single db and clean it before each test
MyIntegrationTestConfig / parallelExecution := false

lazy val backoffice_api = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .configs(MyIntegrationTestConfig)
  .settings(
    Defaults.itSettings,
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      BuildInfoKey.constant("swaggerVersion", swaggerUiVersion)),
    buildInfoPackage := "io.dlinov.auth")

scalaVersion := "2.12.8"

val catsVersion = "1.6.0"
val circeVersion = "0.11.1"
val doobieVersion = "0.6.0"
val http4sVersion = "0.20.0"
val http4sTracerVersion = "1.2.0"
val rhoVersion = "0.19.0-M7"
val monocleVersion = "1.5.1-cats"
val pureConfigVersion = "0.10.2"
val scalaCacheVersion = "0.27.0"
val jwtVersion = "2.1.0"
val macwireVersion = "2.3.2"
val scalaExtVersion = "0.5.3"
val logbackVersion = "1.2.3"
val mariaDbVersion = "2.3.0"
val couchbaseVersion = "2.7.4"
val hadoopVersion = "3.1.2"
val commonsEmailVersion = "1.5"
val commonsTextVersion = "1.6"
val swaggerUiVersion = "3.20.9"
val scalatestVersion = "3.0.5"
val h2Version = "1.4.197"

val AllTest = "it,test"

libraryDependencies ++= Seq(
  "org.mariadb.jdbc" % "mariadb-java-client" % mariaDbVersion,
  "org.apache.commons" % "commons-email" % commonsEmailVersion,
  "org.apache.commons" % "commons-text" % commonsTextVersion,
  "org.cvogt" %% "scala-extensions" % scalaExtVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-cats-effect" % scalaCacheVersion,
  // "com.github.gvolpe" %% "http4s-tracer" % http4sTracerVersion,
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
  "com.github.pureconfig" %% "pureconfig-http4s" % pureConfigVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.pauldijou" %% "jwt-circe" % jwtVersion,
  "org.http4s" %% "rho-swagger" % rhoVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.couchbase.client" % "java-client" % couchbaseVersion,
  "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
  "org.webjars" % "swagger-ui" % swaggerUiVersion,

  "com.softwaremill.macwire" %% "macros" % macwireVersion % Provided,

  "org.scalatest" %% "scalatest" % scalatestVersion % AllTest,
  "com.h2database" % "h2" % h2Version % Test,
  "org.tpolecat" %% "doobie-h2" % doobieVersion % Test,
  "org.tpolecat" %% "doobie-scalatest" % doobieVersion % AllTest,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % AllTest)

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
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
  "-Ywarn-nullary-override")

dockerBaseImage := "openjdk:11-jre-slim"
dockerExposedPorts := Seq(9000)
/*dockerCommands := {
  val dockerCmds = dockerCommands.value
  val setWorkingDir = dockerCmds.indexWhere(_.makeContent.startsWith("WORKDIR "))
  val (cmds1, cmds2) = dockerCmds.splitAt(setWorkingDir + 1)
  cmds1 ++ Seq(
    Cmd("RUN", "apt update && apt install -y ssh telnet tar curl wget")
  ) ++ cmds2
}*/
Docker / daemonUser := "nobody"
Docker / daemonGroup := "nogroup"
Docker / packageName := "auth-api"

scalariformPreferences := scalariformPreferences.value
  .setPreference(DanglingCloseParenthesis, Prevent)
  .setPreference(RewriteArrowSymbols, true) // allows not to force this option in IDE
  .setPreference(SpacesAroundMultiImports, false)
  .setPreference(DoubleIndentConstructorArguments, true) // http://docs.scala-lang.org/style/declarations.html#classes
  .setPreference(NewlineAtEndOfFile, true)

coverageMinimum := 86
coverageFailOnMinimum := false
