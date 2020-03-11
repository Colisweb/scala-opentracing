package com.colisweb.tracing.domain

import java.util.UUID

trait TracingContextBuilder[F[_]] {
  protected def newCorrelationId: String = UUID.randomUUID().toString

  def build(
      operationName: String,
      tags: Tags = Map.empty,
      correlationId: String = newCorrelationId
  ): TracingContextResource[F]
}
