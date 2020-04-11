import CompileFlags._
import Dependencies._

lazy val scala213 = "2.13.1"
lazy val scala212 = "2.12.10"
lazy val supportedScalaVersions = List(scala213, scala212)

ThisBuild / scalaVersion := scala213
ThisBuild / organization := "com.colisweb"
ThisBuild / organizationName := "colisweb"
ThisBuild / bintrayOrganization := Some("colisweb")
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / parallelExecution := false
ThisBuild / scalacOptions ++= crossScalacOptions(scalaVersion.value)

resolvers += Resolver.sonatypeRepo("releases")

lazy val root = (project in file("."))
  .settings(skip in publish := true)
  .aggregate(core, context, httpServer, httpClient, httpTest, amqp)

lazy val amqp = Project(id = "scala-opentracing-amqp", base = file("amqp"))
  .settings(
    libraryDependencies ++= List(fs2Rabbit, TestsDependencies.scalatest)
  )

lazy val core = Project(id = "scala-opentracing-core", base = file("core"))
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Log.all ++ List(Cats.catsEffect)
  )

lazy val context = Project(id = "scala-opentracing-context", base = file("context"))
  .dependsOn(core)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= OpenTracing.all
      ++ Seq(
        scalaCollectionsCompat,
        TestsDependencies.scalatest,
        Cats.cats,
        Cats.catsEffect,
        Http4s.http4sDsl,
        compilerPlugin(kindProjector),
      )
  )

lazy val httpServer =
  Project(id = "scala-opentracing-http4s-server-tapir", base = file("http/server"))
    .settings(
      crossScalaVersions := supportedScalaVersions,
      libraryDependencies ++= Tapir.all ++ Seq(
        compilerPlugin(kindProjector),
        TestsDependencies.scalatest
      )
    )
    .dependsOn(context)

lazy val httpClient =
  Project(id = "scala-opentracing-http4s-client-blaze", base = file("http/client"))
    .dependsOn(context)
    .settings(
      libraryDependencies ++= List(Http4s.blazeClient)
    )

lazy val httpTest = Project(id = "scala-opentracing-http4s-test", base = file("http/test"))
  .dependsOn(httpClient, httpServer)
  .settings(
    libraryDependencies ++= TestsDependencies.utils ++ TestsDependencies.circeAll,
    skip in publish := true
  )


