package com.colisweb.tracing.http4s

import org.http4s.Request
import com.colisweb.tracing.TracingContext

case class TracedRequest[F[_]](
    request: Request[F],
    tracingContext: TracingContext[F]
)
