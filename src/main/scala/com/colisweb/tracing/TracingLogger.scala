package com.colisweb.tracing

import cats.syntax.all._
import cats.effect.Sync
import org.slf4j.{Logger, Marker}
import cats.data.OptionT

object TracingLogger {

  /**
    * A pure logger that will automatically log SpanId and TraceId from
    * a given TracingContext. The logging side effects will be lifted into
    * the tracing context's F monad.
    *
    * You must provide a way of creating a slf4j Marker from the TracingContext.
    */
  def pureTracingLogger[F[_]: Sync](
      logger: Logger,
      tracingContext: TracingContext[F],
      markerFactory: TracingContext[F] => Marker
  ): PureLogger[F] = {
    val pureLogger = PureLogger[F](logger)
    def addCtxMarker(marker: Marker): Marker =
      PureLogger.combineMarkers(Some(marker), Some(markerFactory(tracingContext))).get
    new PureLogger[F] {
      def trace(msg: String, args: Object*): F[Unit] = pureLogger.trace(msg, args: _*)
      def trace(marker: Marker, msg: String, args: Object*): F[Unit] =
        pureLogger.trace(addCtxMarker(marker), msg, args: _*)
      def debug(msg: String, args: Object*): F[Unit] = pureLogger.debug(msg, args: _*)
      def debug(marker: Marker, msg: String, args: Object*): F[Unit] =
        pureLogger.info(addCtxMarker(marker), msg, args: _*)
      def info(msg: String, args: Object*): F[Unit] = pureLogger.info(msg, args: _*)
      def info(marker: Marker, msg: String, args: Object*): F[Unit] =
        pureLogger.info(addCtxMarker(marker), msg, args: _*)
      def warn(msg: String, args: Object*): F[Unit] = pureLogger.warn(msg, args: _*)
      def warn(marker: Marker, msg: String, args: Object*): F[Unit] =
        pureLogger.info(addCtxMarker(marker), msg, args: _*)
      def error(msg: String, args: Object*): F[Unit] = pureLogger.error(msg, args: _*)
      def error(marker: Marker, msg: String, args: Object*): F[Unit] =
        pureLogger.info(addCtxMarker(marker), msg, args: _*)
    }
  }
}
