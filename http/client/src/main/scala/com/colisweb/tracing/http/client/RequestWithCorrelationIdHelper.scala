package com.colisweb.tracing.http.client

import java.util.UUID

import com.colisweb.tracing.core.TracingContext
import org.http4s.{Header, Request}

trait RequestWithCorrelationIdHelper {

  implicit final class RequestWithCorrelationId[F[_]](req: Request[F]) {

    final val correlationIdHeaderName = "X-Correlation-Id"

    private def injectCorrelationIdToRequest(correlationId: String): Request[F] = {
      val correlationIdHeader = Header(correlationIdHeaderName, correlationId)
      req.putHeaders(correlationIdHeader)
    }

    def withCorrelationId: Request[F] = injectCorrelationIdToRequest(UUID.randomUUID().toString)

    def withCorrelationId(context: TracingContext[F]): Request[F] =
      injectCorrelationIdToRequest(context.correlationId)

  }

}
