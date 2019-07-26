package com.colisweb.tracing
import org.slf4j.Marker

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
