package com.colisweb.tracing.context

import cats.effect.{Resource, Sync}
import com.colisweb.tracing.core._
import org.slf4j.Logger

/** A tracing context that does nothing (no measurement, no log). This is useful
  * as a mock implementation for your tests of if you need to disable tracing
  * conditionally
  */
class NoOpTracingContext[F[_]: Sync](override val correlationId: String) extends TracingContext[F] {

  def addTags(tags: Tags): F[Unit] = Sync[F].unit

  def span(operationName: String, tags: Tags): TracingContextResource[F] =
    Resource.pure(NoOpTracingContext(correlationId))

  override def logger(implicit slf4jLogger: Logger): PureLogger[F] = PureLogger[F](slf4jLogger)
}

object NoOpTracingContext {
  def apply[F[_]: Sync](correlationId: String) = new NoOpTracingContext[F](correlationId)

  def builder[F[_]: Sync](): F[TracingContextBuilder[F]] =
    Sync[F].delay((_: String, _: Tags, correlationId: String) => Resource.pure(NoOpTracingContext(correlationId = correlationId)))

}
