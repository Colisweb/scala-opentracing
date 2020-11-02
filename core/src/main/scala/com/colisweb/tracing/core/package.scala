package com.colisweb.tracing

import cats.effect.Resource

package object core {
  type Tags                         = Map[String, String]
  type TracingContextResource[F[_]] = Resource[F, TracingContext[F]]

}
