package com.colisweb

import cats.effect.Resource

package object tracing {
  type Tags = Map[String, String]
  type TracingContextResource[F[_]] = Resource[F, TracingContext[F]]

  trait TracingContextBuilder[F[_]] {
    def apply(
        operationName: String,
        tags: Tags = Map.empty
    ): TracingContextResource[F]
  }
}
