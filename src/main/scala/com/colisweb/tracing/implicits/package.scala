package com.colisweb.tracing

import cats.effect._
import cats.data._
import cats.syntax.all._
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.Logger
import com.colisweb.tracing.utils._

import scala.collection.JavaConverters.mapAsJavaMap

package object implicits {

  implicit class ResourceOps[F[_]: Sync, A](resource: Resource[F, A]) {

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
    def wrap[B](body: => F[B]): F[B] = resource.use(_ => body)

    /** Allows allocating a Resource and supplying it to a function returning
      * an EitherT.
      */
    def either[L, R](body: A => EitherT[F, L, R]): EitherT[F, L, R] =
      EitherT(resource.use(a => body(a).value))

    /** Allows allocating a Resource and supplying it to a function returning
      * an OptionT.
      */
    def option[B](body: A => OptionT[F, B]): OptionT[F, B] =
      OptionT(resource.use(a => body(a).value))
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
