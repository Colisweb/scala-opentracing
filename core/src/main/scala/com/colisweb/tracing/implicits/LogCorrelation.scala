package com.colisweb.tracing.implicits

import com.colisweb.tracing.TracingContext
import cats.effect.Sync
import org.slf4j.Logger
import com.colisweb.tracing.utils.PureLogger
import com.colisweb.tracing.datadog.DDTracingContext
import com.colisweb.tracing.datadog.DDLogCorrelation

trait LogCorrelation {
  implicit class TracingLogger[F[_]: Sync](
      tracingContext: TracingContext[F]
  )(implicit slf4jLogger: Logger) {
    val logger: PureLogger[F] = tracingContext match {
      case c: DDTracingContext[F] => DDLogCorrelation.logger(slf4jLogger, c)
      case _                      => PureLogger[F](slf4jLogger)
    }
  }
}
