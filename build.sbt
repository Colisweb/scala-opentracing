import CompileFlags._

lazy val scala213               = "2.13.6"
lazy val scala212               = "2.12.14"
lazy val supportedScalaVersions = List(scala213, scala212)

ThisBuild / scalaVersion := scala213
ThisBuild / parallelExecution := false
ThisBuild / scalacOptions ++= crossScalacOptions(scalaVersion.value)
ThisBuild / pushRemoteCacheTo := Some(
  MavenCache("local-cache", baseDirectory.value / sys.env.getOrElse("CACHE_PATH", "sbt-cache"))
)
resolvers += Resolver.sonatypeRepo("releases")

lazy val root = (project in file(".")).settings(skip in publish := true).aggregate(core, context, httpServer, httpClient, httpTest, amqp)

lazy val amqp = Project(id = "scala-opentracing-amqp", base = file("amqp")).settings(
  crossScalaVersions := supportedScalaVersions,
  libraryDependencies ++= Seq(
    CompileTimeDependencies.fs2Rabbit,
    TestsDependencies.scalatest
  )
)

lazy val core = Project(id = "scala-opentracing-core", base = file("core")).settings(
  crossScalaVersions := supportedScalaVersions,
  libraryDependencies ++= Seq(
    CompileTimeDependencies.catsEffect,
    CompileTimeDependencies.log4catsSlf4j
  )
)

lazy val context = Project(id = "scala-opentracing-context", base = file("context"))
  .dependsOn(core)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      CompileTimeDependencies.cats,
      CompileTimeDependencies.opentracingApi,
      CompileTimeDependencies.opentracingUtil,
      CompileTimeDependencies.opentracingDd,
      CompileTimeDependencies.scalaCompat,
      CompileTimeDependencies.scalaLogging,
      CompileTimeDependencies.logstashLogbackEncoder,
      TestsDependencies.scalatest,
      TestsDependencies.logback,
      compilerPlugin(CompileTimeDependencies.kindProjector)
    )
  )

lazy val httpServer = Project(id = "scala-opentracing-http4s-server-tapir", base = file("http/server"))
  .dependsOn(context % "test->test;compile->compile")
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      CompileTimeDependencies.tapir,
      CompileTimeDependencies.tapirHttp4sServer,
      compilerPlugin(CompileTimeDependencies.kindProjector)
    )
  )

lazy val httpClient = Project(id = "scala-opentracing-http4s-client-blaze", base = file("http/client"))
  .dependsOn(context)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(CompileTimeDependencies.http4s)
  )

lazy val httpTest = Project(id = "scala-opentracing-http4s-test", base = file("http/test"))
  .dependsOn(httpClient, httpServer % "test->test;compile->compile")
  .settings(
    libraryDependencies ++= Seq(
      TestsDependencies.circe,
      TestsDependencies.circeGeneric,
      TestsDependencies.circeGenericExtras,
      TestsDependencies.circeHttp4sCirce,
      TestsDependencies.tapirJsonCirce,
      TestsDependencies.requests,
      TestsDependencies.wiremock,
      TestsDependencies.http4sBlazeClient
    ),
    skip in publish := true
  )
