package com.colisweb.tracing.datadog

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.colisweb.tracing.OpenTracingContext
import com.colisweb.tracing.TracingContext._
import datadog.opentracing._
import datadog.trace.api.DDTags.SERVICE_NAME
import io.opentracing.util.GlobalTracer
import com.colisweb.tracing.TracingContext

/**
  * This tracing context is intended to be used with Datadog APM.
  * It adds the Service Name tag to all spans, required to see the traces in the APM
  * view of Datadog.
  * It also provides access to span id and trace id to correlate logs and traces together.
  */
class DDTracingContext[F[_]: Sync](
    protected val tracer: DDTracer,
    protected val span: DDSpan,
    protected val serviceName: String
) extends TracingContext[F] {

  override def traceId =
    OptionT.liftF(Sync[F] delay {
      span.getTraceId()
    })

  override def spanId =
    OptionT.liftF(Sync[F] delay {
      span.getSpanId()
    })

  override def childSpan(operationName: String, tags: Map[String, String] = Map.empty) =
    DDTracingContext.apply[F](tracer, serviceName, Some(span))(operationName, tags)

  def addTags(tags: Map[String, String]): F[Unit] = Sync[F].delay {
    tags.foreach {
      case (key, value) => span.setTag(key, value)
    }
  }
}

object DDTracingContext {
  def apply[F[_]: Sync](tracer: DDTracer, serviceName: String, parentSpan: Option[DDSpan] = None)(
      operationName: String,
      tags: Map[String, String] = Map.empty
  ): TracingContextResource[F] =
    OpenTracingContext
      .spanResource(tracer, operationName, parentSpan)
      .map(new DDTracingContext(tracer, _, serviceName))
      .evalMap(ctx => ctx.addTags(tags + (SERVICE_NAME -> serviceName)).map(_ => ctx))

  private def buildAndRegisterDDTracer[F[_]: Sync] = Sync[F].delay {
    val tracer = new DDTracer()
    GlobalTracer.register(tracer)
    datadog.trace.api.GlobalTracer.registerIfAbsent(tracer)
    tracer
  }

  def getDDTracingContextBuilder[F[_]: Sync](serviceName: String): F[TracingContextBuilder[F]] =
    buildAndRegisterDDTracer.map(tracer => apply(tracer, serviceName))

}
