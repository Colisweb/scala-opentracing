package com.colisweb.tracing.context

import cats.effect.{Resource, Sync}
import com.colisweb.tracing.domain.{PureLogger, Tags}
import org.slf4j.Logger

/**
  * A tracing context that does nothing (no measurement, no log). This is useful
  * as a mock implementation for your tests of if you need to disable tracing
  * conditionally
  */
class NoOpTracingContext[F[_]: Sync] extends TracingContext[F] {

  def addTags(tags: Tags): F[Unit] = Sync[F].unit

  def childSpan(operationName: String, tags: Tags): TracingContextResource[F] =
    Resource.pure(NoOpTracingContext())

  override def logger(implicit slf4jLogger: Logger): PureLogger[F] = PureLogger[F](slf4jLogger)

  override def correlationId: String = "no-op-tracing-context"
}

object NoOpTracingContext {
  def apply[F[_]: Sync]() = new NoOpTracingContext[F]

  def getNoOpTracingContextBuilder[F[_]: Sync]: F[TracingContextBuilder[F]] =
    Sync[F].pure((_, _) => Resource.pure(NoOpTracingContext[F]()))
}
