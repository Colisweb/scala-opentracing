import sbt._

object Dependencies {
  final val fs2Rabbit = "dev.profunktor" %% "fs2-rabbit" % "2.1.1"
  final val kindProjector = "org.typelevel" %% "kind-projector" % "0.10.3"

  object Cats {
    final val cats = "org.typelevel" %% "cats-core" % "2.0.0"
    final val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"
  }

  object OpenTracing {
    final val opentracing = "0.33.0"
    final val api = "io.opentracing" % "opentracing-api" % opentracing
    final val util = "io.opentracing" % "opentracing-util" % opentracing
    final val dd = "com.datadoghq" % "dd-trace-ot" % "0.45.0"
    final val all = Seq(api, util, dd)
  }

  object Tapir {
    final val version = "0.12.23"
    final val core = "com.softwaremill.sttp.tapir" %% "tapir-core" % version
    final val http4sServer = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % version
    final val all = Seq(core, http4sServer)
  }

  object Http4s {
    final val version = "0.21.1"

    final val blazeClient = "org.http4s" %% "http4s-blaze-client" % version
    final val core = "org.http4s" %% "http4s-core" % version
    final val http4sDsl = "org.http4s" %% "http4s-dsl" % "0.20.19"

    final val all = List(blazeClient, core)
  }

  object Log {
    final val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
    final val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    final val logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "6.3"

    final val all = Seq(
      logback,
      scalaLogging,
      logstashLogbackEncoder
    )
  }
}

object TestsDependencies {
  import Dependencies._

  final val circeVersion = "0.13.0"

  final val core = "io.circe" %% "circe-core" % circeVersion % Test
  final val generic = "io.circe" %% "circe-generic" % circeVersion % Test
  final val genericExtras = "io.circe" %% "circe-generic-extras" % circeVersion % Test
  final val http4sCirce = "org.http4s" %% "http4s-circe" % Http4s.version % Test
  final val tapirJsonCirce =
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Tapir.version % Test

  final val circeAll = List(core, generic, genericExtras, http4sCirce, tapirJsonCirce)

  final val requests = "com.lihaoyi" %% "requests" % "0.2.0" % Test
  final val scalatest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
  final val wiremock = "com.github.tomakehurst" % "wiremock" % "2.25.1" % Test

  final val utils = List(requests, scalatest, wiremock)
}
