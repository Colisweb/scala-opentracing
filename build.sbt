import Dependencies._

lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / scalaVersion := scala212
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.colisweb"
ThisBuild / organizationName := "colisweb"

lazy val root = (project in file("."))
  .settings(
    name := "Scala Opentracing",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= OpenTracing.all
      ++ Log.all
      ++ Seq(
        scalaTest % Test,
        cats,
        catsEffect,
        http4sDsl
      )
  )
