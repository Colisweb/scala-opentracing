package com.colisweb.tracing
import org.slf4j.Marker
import cats.effect.Sync

/**
  * A logger that wraps the side-effect of logging into
  * some algebraic effect F
  */
trait PureLogger[F[_]] {
  def trace(msg: String, args: Object*): F[Unit]
  def trace(marker: Marker, msg: String, args: Object*): F[Unit]
  def debug(msg: String, args: Object*): F[Unit]
  def debug(marker: Marker, msg: String, args: Object*): F[Unit]
  def info(msg: String, args: Object*): F[Unit]
  def info(marker: Marker, msg: String, args: Object*): F[Unit]
  def warn(msg: String, args: Object*): F[Unit]
  def warn(marker: Marker, msg: String, args: Object*): F[Unit]
  def error(msg: String, args: Object*): F[Unit]
  def error(marker: Marker, msg: String, args: Object*): F[Unit]
}

object PureLogger {
  def combineMarkers(a: Option[Marker], b: Option[Marker]): Option[Marker] =
    for {
      firstMaker <- a
      secondMarker <- b
    } yield { firstMaker.add(secondMarker); firstMaker }

  type LoggingFunction[F[_]] = (Option[Marker], String, Object*) => F[Unit]

  private def wrapSlf4j[F[_]: Sync](
      logMethod: (String, Object*) => Unit,
      logMethodWithMarker: (Marker, String, Object*) => Unit
  )(marker: Option[Marker], msg: String, args: Object*) = Sync[F] delay {
    marker match {
      case Some(marker) => logMethodWithMarker(marker, msg, args: _*)
      case None         => logMethod(msg, args: _*)
    }
  }

  def apply[F[_]: Sync](logger: org.slf4j.Logger): PureLogger[F] = {
    val wrappedTrace: LoggingFunction[F] = wrapSlf4j[F](logger.trace, logger.trace)
    val wrappedDebug: LoggingFunction[F] = wrapSlf4j[F](logger.debug, logger.debug)
    val wrappedInfo: LoggingFunction[F] = wrapSlf4j[F](logger.info, logger.info)
    val wrappedWarn: LoggingFunction[F] = wrapSlf4j[F](logger.warn, logger.warn)
    val wrappedError: LoggingFunction[F] = wrapSlf4j[F](logger.error, logger.error)
    new PureLogger[F] {

      def trace(marker: Marker, msg: String, args: Object*): F[Unit] =
        wrappedTrace(Some(marker), msg, args: _*)
      def trace(msg: String, args: Object*): F[Unit] = wrappedTrace(None, msg, args: _*)

      def debug(marker: Marker, msg: String, args: Object*): F[Unit] =
        wrappedDebug(Some(marker), msg, args: _*)
      def debug(msg: String, args: Object*): F[Unit] = wrappedDebug(None, msg, args: _*)

      def info(marker: Marker, msg: String, args: Object*): F[Unit] =
        wrappedInfo(Some(marker), msg, args: _*)
      def info(msg: String, args: Object*): F[Unit] = wrappedInfo(None, msg, args: _*)

      def warn(marker: Marker, msg: String, args: Object*): F[Unit] =
        wrappedWarn(Some(marker), msg, args: _*)
      def warn(msg: String, args: Object*): F[Unit] = wrappedWarn(None, msg, args: _*)

      def error(marker: Marker, msg: String, args: Object*): F[Unit] =
        wrappedError(Some(marker), msg, args: _*)
      def error(msg: String, args: Object*): F[Unit] = wrappedError(None, msg, args: _*)
    }

  }
}
