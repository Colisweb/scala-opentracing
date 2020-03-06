package com.colisweb.tracing.context

import com.colisweb.tracing.domain.Tags

trait TracingContextBuilder[F[_]] {
  def apply(operationName: String, tags: Tags = Map.empty): TracingContextResource[F]
}
