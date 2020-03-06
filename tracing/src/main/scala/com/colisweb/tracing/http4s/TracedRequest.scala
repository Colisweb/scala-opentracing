package com.colisweb.tracing.http4s

import com.colisweb.tracing.context.TracingContext
import org.http4s.Request

case class TracedRequest[F[_]](
    request: Request[F],
    tracingContext: TracingContext[F]
)
