import Dependencies._
import CompileFlags._
import ReleaseTransformations._

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

lazy val root = (project in file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(core, tapir)

lazy val core = (project in file("core"))
  .settings(
    name := "Scala Opentracing",
    bintrayPackage := "scala-opentracing",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= OpenTracing.all
      ++ Log.all
      ++ Seq(
        scalaTest % Test,
        cats,
        catsEffect,
        http4sDsl,
        compilerPlugin(kindProjector)
      )
  )

lazy val tapir = (project in file("tapir"))
  .settings(
    name := "Scala Opentracing Tapir Integration",
    bintrayPackage := "scala-opentracing-tapir",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Tapir.all ++ Seq(
      compilerPlugin(kindProjector),
      scalaTest % Test
    )
  )
  .dependsOn(core)

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