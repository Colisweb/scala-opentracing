package com.colisweb.tracing

import cats.effect.{Resource, Sync}
import com.colisweb.tracing.TracingContext.TracingContextResource

class NoOpTracingContext[F[_]: Sync] extends TracingContext[F] {

  def addTags(tags: Map[String, String]): F[Unit] = Sync[F].unit

  def childSpan(operationName: String, tags: Map[String, String]): TracingContextResource[F] =
    Resource.pure(NoOpTracingContext())

}

object NoOpTracingContext {
  def apply[F[_]: Sync]() = new NoOpTracingContext[F]
}
