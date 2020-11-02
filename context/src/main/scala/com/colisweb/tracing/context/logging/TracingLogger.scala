package com.colisweb.tracing.context.logging

import cats.effect.Sync
import cats.syntax.all._
import com.colisweb.tracing.core.PureLogger
import org.slf4j.{Logger, Marker}

object TracingLogger {

  /** A pure logger that will automatically log SpanId and TraceId from
    * a given TracingContext. The logging side effects will be lifted into
    * the tracing context's F monad.
    *
    * You must provide a way of creating a slf4j Marker from the TracingContext.
    */
  def pureTracingLogger[F[_]: Sync](
      logger: Logger,
      ctxMarker: F[Marker]
  ): PureLogger[F] = {
    val pureLogger = PureLogger[F](logger)
    def addCtxMarker(marker: Option[Marker]): F[Marker] =
      ctxMarker.map(m => combineMarkers(marker, m))

    new PureLogger[F] {
      def trace(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.trace(m, msg, args: _*))
      def trace(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.trace(m, msg, args: _*))
      def trace(msg: String, throwable: Throwable): F[Unit] =
        addCtxMarker(None).flatMap(_ => pureLogger.trace(msg, throwable))

      def debug(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.debug(m, msg, args: _*))
      def debug(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.debug(m, msg, args: _*))
      def debug(msg: String, throwable: Throwable): F[Unit] =
        addCtxMarker(None).flatMap(_ => pureLogger.debug(msg, throwable))

      def info(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.info(m, msg, args: _*))
      def info(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.info(m, msg, args: _*))
      def info(msg: String, throwable: Throwable): F[Unit] =
        addCtxMarker(None).flatMap(_ => pureLogger.info(msg, throwable))

      def warn(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.warn(m, msg, args: _*))
      def warn(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.warn(m, msg, args: _*))
      def warn(msg: String, throwable: Throwable): F[Unit] =
        addCtxMarker(None).flatMap(_ => pureLogger.warn(msg, throwable))

      def error(msg: String, args: Object*): F[Unit] =
        addCtxMarker(None).flatMap(m => pureLogger.error(m, msg, args: _*))
      def error(marker: Marker, msg: String, args: Object*): F[Unit] =
        addCtxMarker(Some(marker)).flatMap(m => pureLogger.error(m, msg, args: _*))
      def error(msg: String, throwable: Throwable): F[Unit] =
        addCtxMarker(None).flatMap(_ => pureLogger.error(msg, throwable))

    }
  }

  def combineMarkers(a: Option[Marker], b: Marker): Marker =
    a match {
      case Some(a) => { a.add(b); a }
      case _       => b
    }
}
