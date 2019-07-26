package com.colisweb.tracing

import cats.effect._
import org.slf4j.{Logger, Marker}

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
  ): PureLogger[F] = new PureLogger[F] {
    def combineMarkers(a: Marker, b: Option[Marker]): Marker = b match {
      case Some(m) => { a.add(m); a }
      case _       => a
    }

    type LoggingFunction = (Option[Marker], String, Object*) => F[Unit]

    def wrapSlf4j(
        slf4jMethod: (Marker, String, Object*) => Unit
    )(marker: Option[Marker], msg: String, args: Object*): F[Unit] = {
      Sync[F].flatMap(markerFactory(tracingContext))(
        m =>
          Sync[F].delay {
            slf4jMethod(combineMarkers(m, marker), msg, args: _*)
          }
      )
    }

    val trace: LoggingFunction                                     = wrapSlf4j(logger.trace)
    def trace(marker: Marker, msg: String, args: Object*): F[Unit] = trace(Some(marker), msg, args: _*)
    def trace(msg: String, args: Object*): F[Unit]                 = trace(None, msg, args: _*)

    val debug: LoggingFunction                                     = wrapSlf4j(logger.debug)
    def debug(marker: Marker, msg: String, args: Object*): F[Unit] = debug(Some(marker), msg, args: _*)
    def debug(msg: String, args: Object*): F[Unit]                 = debug(None, msg, args: _*)

    val info: LoggingFunction                                     = wrapSlf4j(logger.info)
    def info(marker: Marker, msg: String, args: Object*): F[Unit] = info(Some(marker), msg, args: _*)
    def info(msg: String, args: Object*): F[Unit]                 = info(None, msg, args: _*)

    val warn: LoggingFunction                                     = wrapSlf4j(logger.warn)
    def warn(marker: Marker, msg: String, args: Object*): F[Unit] = warn(Some(marker), msg, args: _*)
    def warn(msg: String, args: Object*): F[Unit]                 = warn(None, msg, args: _*)

    val error: LoggingFunction                                     = wrapSlf4j(logger.error)
    def error(marker: Marker, msg: String, args: Object*): F[Unit] = error(Some(marker), msg, args: _*)
    def error(msg: String, args: Object*): F[Unit]                 = error(None, msg, args: _*)
  }

}
