package com.colisweb.tracing

import cats.effect._
import cats.syntax.all._
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.Logger
import com.colisweb.tracing.utils._

import scala.collection.JavaConverters.mapAsJavaMap

package object implicits {

  /** Allows ignoring the value of a Resource.
    * {{{
    * import com.colisweb.tracing.implicits._
    *
    * someTracingContext.childContext("Child operation") wrap F.delay { /* Some computation */ }
    * }}}
    *
    * is equivalent to
    * {{{
    * someTracingContext.childContext("Child operation") use { _ => F.delay { /* Some computation */ } }
    * }}}
    */
  implicit class ResourceOps[F[_]: Sync, A](resource: Resource[F, A]) {
    def wrap[B](body: => F[B]) = resource.use(_ => body)
  }

  /**
    * This allows creating a PureLogger[F] from an SLF4J Logger. The
    * logger will automatically add "ss.span_id" and "dd.trace_id" from the current
    * TracingContext to log output. This enables to retrieve logs related to a given trace in Datadog.
    */
  implicit class DatadogJsonTracingLogger[F[_]: Sync](
      tracingContext: TracingContext[F]
  )(implicit slf4jLogger: Logger) {
    val logger: PureLogger[F] = TracingLogger.pureTracingLogger[F](
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
}
