package com.colisweb.tracing.http.server

import com.colisweb.tracing.domain.TracingContext
import org.http4s.Request

case class TracedRequest[F[_]](request: Request[F], tracingContext: TracingContext[F])
