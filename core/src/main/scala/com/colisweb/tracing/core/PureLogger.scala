package com.colisweb.tracing.core

import cats.effect.Sync
import org.slf4j.Marker

/** A logger that wraps the side-effect of logging into
  * some algebraic effect F
  */
trait PureLogger[F[_]] {
  def trace(msg: String, args: Object*): F[Unit]
  def trace(marker: Marker, msg: String, args: Object*): F[Unit]
  def trace(msg: String, throwable: Throwable): F[Unit]

  def debug(msg: String, args: Object*): F[Unit]
  def debug(marker: Marker, msg: String, args: Object*): F[Unit]
  def debug(msg: String, throwable: Throwable): F[Unit]

  def info(msg: String, args: Object*): F[Unit]
  def info(marker: Marker, msg: String, args: Object*): F[Unit]
  def info(msg: String, throwable: Throwable): F[Unit]

  def warn(msg: String, args: Object*): F[Unit]
  def warn(marker: Marker, msg: String, args: Object*): F[Unit]
  def warn(msg: String, throwable: Throwable): F[Unit]

  def error(msg: String, args: Object*): F[Unit]
  def error(marker: Marker, msg: String, args: Object*): F[Unit]
  def error(msg: String, throwable: Throwable): F[Unit]
}

object PureLogger {

  def apply[F[_]: Sync](logger: org.slf4j.Logger): PureLogger[F] =
    new PureLogger[F] {

      def trace(marker: Marker, msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.trace(marker, msg, args: _*)
      }
      def trace(msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.trace(msg, args: _*)
      }
      def trace(msg: String, throwable: Throwable): F[Unit] = Sync[F].delay {
        logger.trace(msg, throwable)
      }

      def debug(marker: Marker, msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.debug(marker, msg, args: _*)
      }
      def debug(msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.debug(msg, args: _*)
      }
      def debug(msg: String, throwable: Throwable): F[Unit] = Sync[F].delay {
        logger.debug(msg, throwable)
      }

      def info(marker: Marker, msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.info(marker, msg, args: _*)
      }
      def info(msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.info(msg, args: _*)
      }
      def info(msg: String, throwable: Throwable): F[Unit] = Sync[F].delay {
        logger.info(msg, throwable)
      }

      def warn(marker: Marker, msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.warn(marker, msg, args: _*)
      }
      def warn(msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.warn(msg, args: _*)
      }
      def warn(msg: String, throwable: Throwable): F[Unit] = Sync[F].delay {
        logger.warn(msg, throwable)
      }

      def error(marker: Marker, msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.error(marker, msg, args: _*)
      }
      def error(msg: String, args: Object*): F[Unit] = Sync[F].delay {
        logger.error(msg, args: _*)
      }
      def error(msg: String, throwable: Throwable): F[Unit] = Sync[F].delay {
        logger.error(msg, throwable)
      }
    }

}
