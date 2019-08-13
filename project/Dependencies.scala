import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  val cats = "org.typelevel" %% "cats-core" % "1.4.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "1.3.0"
  val http4sDsl = "org.http4s" %% "http4s-dsl" % "0.20.0"

  object OpenTracing {
    val version = "0.31.0"
    val api = "io.opentracing" % "opentracing-api" % version
    val util = "io.opentracing" % "opentracing-util" % version
    val dd = "com.datadoghq" % "dd-trace-ot" % "0.30.0"
    val all = Seq(api, util, dd)
  }

  object Tapir {
    final val version = "0.9.1"
    final val core = "com.softwaremill.tapir" %% "tapir-core" % version
    final val http4sServer = "com.softwaremill.tapir" %% "tapir-http4s-server" % version
    final val all = Seq(core, http4sServer)
  }

  object Log {
    val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    val logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "6.1"

    val all = Seq(
      logback,
      scalaLogging,
      logstashLogbackEncoder
    )
  }

}
