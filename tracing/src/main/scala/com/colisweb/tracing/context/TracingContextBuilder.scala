package com.colisweb.tracing.context

import java.util.UUID

import com.colisweb.tracing.domain.Tags

trait TracingContextBuilder[F[_]] {
  protected def newCorrelationId: String = UUID.randomUUID().toString

  def build(
      operationName: String,
      tags: Tags = Map.empty,
      correlationId: String = newCorrelationId
  ): TracingContextResource[F]
}
