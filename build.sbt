import CompileFlags._
import Dependencies._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

lazy val scala212 = "2.12.9"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / scalaVersion := scala212
ThisBuild / organization := "com.colisweb"
ThisBuild / organizationName := "colisweb"
ThisBuild / bintrayOrganization := Some("colisweb")
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / parallelExecution := false
ThisBuild / scalacOptions ++= flags

// Release settings
ThisBuild / releaseCommitMessage := s"[ci skip] Setting version to ${(version in ThisBuild).value}"
ThisBuild / releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepTask(publish),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

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
        TestsDependencies.scalatest,
        Cats.cats,
        Cats.catsEffect,
        Http4s.http4sDsl,
        compilerPlugin(kindProjector)
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
    scalacOptions ++= Seq("-Ypartial-unification"),
    skip in publish := true
  )
