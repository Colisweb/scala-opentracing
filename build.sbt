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
  .aggregate(logging, tracing, httpServer, httpClient, httpTest, amqp)

lazy val logging = Project(id = "scala-opentracing-logging", base = file("logging"))
  .settings(
    name := "Scala Opentracing Logging",
    bintrayPackage := "scala-opentracing-logging",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Log.all ++ List(Cats.catsEffect)
  )

lazy val tracing = Project(id = "scala-opentracing", base = file("tracing"))
  .dependsOn(logging)
  .settings(
    name := "Scala Opentracing",
    bintrayPackage := "scala-opentracing",
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
      name := "Scala Opentracing Tapir Integration",
      bintrayPackage := "scala-opentracing-http4s-server-tapir",
      crossScalaVersions := supportedScalaVersions,
      libraryDependencies ++= Tapir.all ++ Seq(
        compilerPlugin(kindProjector),
        TestsDependencies.scalatest
      )
    )
    .dependsOn(tracing)

lazy val httpClient =
  Project(id = "scala-opentracing-http4s-client-blaze", base = file("http/client"))
    .dependsOn(tracing)
    .settings(
      name := "Scala Opentracing Blaze Integration",
      bintrayPackage := "scala-opentracing-http4s-client-blaze",
      libraryDependencies ++= List(Http4s.blazeClient)
    )

lazy val httpTest = Project(id = "scala-opentracing-http4s-test", base = file("http/test"))
  .dependsOn(httpClient, httpServer)
  .settings(
    libraryDependencies ++= TestsDependencies.utils ++ TestsDependencies.circeAll,
    scalacOptions ++= Seq("-Ypartial-unification"),
    skip in publish := true
  )

lazy val amqp = Project(id = "scala-opentracing-amqp", base = file("amqp"))
  .dependsOn(tracing)
  .settings(
    libraryDependencies ++= List(fs2Rabbit, TestsDependencies.scalatest)
  )
