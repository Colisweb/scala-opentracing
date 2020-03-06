package com.colisweb.tracing.datadog

import cats.effect._
import cats.syntax.all._
import com.colisweb.tracing.context.TracingContext
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.Logger
import com.colisweb.tracing.domain.PureLogger
import com.colisweb.tracing.logging.TracingLogger

import scala.collection.JavaConverters.mapAsJavaMap

object DDLogCorrelation {
  /**
    * This allows creating a PureLogger[F] from an SLF4J Logger. The
    * logger will automatically add "ss.span_id" and "dd.trace_id" from the current
    * TracingContext to log output. This enables to retrieve logs related to a given trace in Datadog.
    */
  def logger[F[_]: Sync](slf4jLogger: Logger, tracingContext: TracingContext[F]): PureLogger[F] =
    TracingLogger.pureTracingLogger[F](
      slf4jLogger,
      tracingContext,
      context => {
        val traceIdMarker = context.traceId
          .map(id => Map("dd.trace_id" -> id))
          .getOrElse(Map.empty)
        val spanIdMarker =
          context.spanId.map(id => Map("dd.span_id" -> id)).getOrElse(Map.empty)
        for {
          spanId <- spanIdMarker
          traceId <- traceIdMarker
        } yield
          appendEntries(
            mapAsJavaMap(traceId ++ spanId)
          )
      }
    )
}
