import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.colisweb"
ThisBuild / organizationName := "colisweb"

lazy val root = (project in file("."))
  .settings(
    name := "Scala Opentracing",
    libraryDependencies += scalaTest % Test
  )
