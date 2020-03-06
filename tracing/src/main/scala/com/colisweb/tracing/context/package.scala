package com.colisweb.tracing

import cats.effect.Resource

package object context {
  type TracingContextResource[F[_]] = Resource[F, TracingContext[F]]
}
