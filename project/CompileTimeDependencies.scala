import sbt._

object Versions {

  final val cats            = "2.1.1"
  final val catsEffect      = "2.1.3"
  final val circe           = "0.13.0"
  final val datadog         = "0.54.0"
  final val fs2             = "2.1.1"
  final val http4s          = "0.21.6"
  final val kindProjector   = "0.11.0"
  final val logback         = "1.2.3"
  final val logstash        = "6.4"
  final val log4cats        = "1.1.1"
  final val opentracing     = "0.33.0"
  final val requests        = "0.6.2"
  final val scalaCompat     = "2.1.6"
  final val scalaLogging    = "3.9.2"
  final val scalatest       = "3.2.0"
  final val scalaCollection = "2.1.4"
  final val tapir           = "0.16.1"
  final val wiremock        = "2.27.1"
}

object CompileTimeDependencies {
  final val cats                   = "org.typelevel"               %% "cats-core"                % Versions.cats
  final val catsEffect             = "org.typelevel"               %% "cats-effect"              % Versions.catsEffect
  final val fs2Rabbit              = "dev.profunktor"              %% "fs2-rabbit"               % Versions.fs2
  final val http4s                 = "org.http4s"                  %% "http4s-core"              % Versions.http4s
  final val kindProjector          = "org.typelevel"               %% "kind-projector"           % Versions.kindProjector cross CrossVersion.full
  final val logstashLogbackEncoder = "net.logstash.logback"         % "logstash-logback-encoder" % Versions.logstash
  final val log4catsSlf4j          = "io.chrisdavenport"           %% "log4cats-slf4j"           % Versions.log4cats
  final val opentracingApi         = "io.opentracing"               % "opentracing-api"          % Versions.opentracing
  final val opentracingDd          = "com.datadoghq"                % "dd-trace-ot"              % Versions.datadog
  final val opentracingUtil        = "io.opentracing"               % "opentracing-util"         % Versions.opentracing
  final val scalaCollectionsCompat = "org.scala-lang.modules"      %% "scala-collection-compat"  % Versions.scalaCollection
  final val scalaLogging           = "com.typesafe.scala-logging"  %% "scala-logging"            % Versions.scalaLogging
  final val tapir                  = "com.softwaremill.sttp.tapir" %% "tapir-core"               % Versions.tapir
  final val tapirHttp4sServer      = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % Versions.tapir
  final val scalaCompat            = "org.scala-lang.modules"      %% "scala-collection-compat"  % Versions.scalaCompat
}

object TestsDependencies {

  final val circe              = "io.circe"                    %% "circe-core"           % Versions.circe     % Test
  final val circeGeneric       = "io.circe"                    %% "circe-generic"        % Versions.circe     % Test
  final val circeGenericExtras = "io.circe"                    %% "circe-generic-extras" % Versions.circe     % Test
  final val circeHttp4sCirce   = "org.http4s"                  %% "http4s-circe"         % Versions.http4s    % Test
  final val http4sBlazeClient  = "org.http4s"                  %% "http4s-blaze-client"  % Versions.http4s    % Test
  final val logback            = "ch.qos.logback"               % "logback-classic"      % Versions.logback   % Test
  final val requests           = "com.lihaoyi"                 %% "requests"             % Versions.requests  % Test
  final val scalatest          = "org.scalatest"               %% "scalatest"            % Versions.scalatest % Test
  final val tapirJsonCirce     = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"     % Versions.tapir     % Test
  final val wiremock           = "com.github.tomakehurst"       % "wiremock"             % Versions.wiremock  % Test
}
