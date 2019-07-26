package com.colisweb.tracing

import cats.effect._
import cats.syntax.all._
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.Logger

import scala.collection.JavaConverters.mapAsJavaMap

package object implicits {

  implicit class ResourceOps[F[_]: Sync, A](resource: Resource[F, A]) {
    def wrap[B](body: => F[B]) = resource.use(_ => body)
  }

  /**
    * This will add the trace_id and span_id to the log output as
    * additional JSON fields, assuming the Logstash encoder is used
    */
  implicit class DatadogJsonTracingLogger[F[_]: Sync](tracingContext: TracingContext[F])(implicit slf4jLogger: Logger) {
    val logger: PureLogger[F] = TracingLogger.pureTracingLogger[F](
      slf4jLogger,
      tracingContext,
      context => {
        val traceIdMarker = context.traceId.map(id => Map("dd.trace_id" -> id)).getOrElse(Map.empty)
        val spanIdMarker  = context.spanId.map(id => Map("dd.span_id"   -> id)).getOrElse(Map.empty)
        for {
          spanId  <- spanIdMarker
          traceId <- traceIdMarker
        } yield appendEntries(
          mapAsJavaMap(traceId ++ spanId)
        )
      }
    )
  }
}
