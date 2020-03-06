package com.colisweb.tracing.context

trait TracingContextBuilder[F[_]] {
  def apply(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F]
}
