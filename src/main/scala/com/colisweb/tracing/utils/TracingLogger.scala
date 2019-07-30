package com.colisweb.tracing.utils

import cats.syntax.all._
import cats.effect.Sync
import org.slf4j.{Logger, Marker}
import com.colisweb.tracing._

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
      markerFactory: TracingContext[F] => F[Marker]
  ): PureLogger[F] = {
    val pureLogger = PureLogger[F](logger)
    def addCtxMarker(marker: Option[Marker]): F[Marker] =
      markerFactory(tracingContext).map(m => combineMarkers(marker, m))

    new PureLogger[F] {
      def trace(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.trace(m, msg, args: _*))
      def trace(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.trace(m, msg, args: _*))

      def debug(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.debug(m, msg, args: _*))
      def debug(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.debug(m, msg, args: _*))

      def info(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.info(m, msg, args: _*))
      def info(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.info(m, msg, args: _*))

      def warn(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.warn(m, msg, args: _*))
      def warn(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.warn(m, msg, args: _*))

      def error(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.error(m, msg, args: _*))
      def error(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.error(m, msg, args: _*))

    }
  }

  def combineMarkers(a: Option[Marker], b: Marker): Marker =
    a match {
      case Some(a) => { a.add(b); a }
      case _       => b
    }
}
