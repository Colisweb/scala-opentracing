package com.colisweb.tracing.datadog

import java.util.UUID

import _root_.datadog.opentracing._
import _root_.datadog.trace.api.DDTags.SERVICE_NAME
import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.colisweb.tracing.context.{
  OpenTracingContext,
  TracingContext,
  TracingContextBuilder,
  TracingContextResource
}
import com.colisweb.tracing.domain.{PureLogger, Tags}
import com.colisweb.tracing.logging.TracingLogger
import com.typesafe.scalalogging.StrictLogging
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.{Logger, Marker}

import scala.collection.JavaConverters.mapAsJavaMap

/**
  * This tracing context is intended to be used with Datadog APM.
  * It adds the Service Name tag to all spans, required to see the traces in the APM
  * view of Datadog.
  * It also provides access to span id and trace id to correlate logs and traces together.
  */
class DDTracingContext[F[_]: Sync](
    protected val tracer: DDTracer,
    protected val span: DDSpan,
    protected val serviceName: String,
    override val correlationId: String
) extends TracingContext[F] {

  override def traceId: OptionT[F, String] =
    OptionT.liftF(Sync[F] delay {
      span.getTraceId
    })

  override def spanId: OptionT[F, String] =
    OptionT.liftF(Sync[F] delay {
      span.getSpanId
    })

  override def childSpan(operationName: String, tags: Tags = Map.empty): TracingContextResource[F] =
    DDTracingContext.apply[F](tracer, serviceName, Some(span), correlationId)(operationName, tags)

  override def addTags(tags: Tags): F[Unit] = Sync[F].delay {
    tags.foreach {
      case (key, value) => span.setTag(key, value)
    }
  }

  override def logger(implicit slf4jLogger: Logger): PureLogger[F] =
    TracingLogger.pureTracingLogger[F](slf4jLogger, markers)

  private lazy val markers: F[Marker] = {

    val traceIdMarker = traceId
      .map(id => Map("dd.trace_id" -> id))
      .getOrElse(Map.empty)
    val spanIdMarker =
      spanId.map(id => Map("dd.span_id" -> id)).getOrElse(Map.empty)
    for {
      spanId <- spanIdMarker
      traceId <- traceIdMarker
    } yield appendEntries(
      mapAsJavaMap(traceId ++ spanId)
    )

  }
}

object DDTracingContext extends StrictLogging {
  def apply[F[_]: Sync](
      tracer: DDTracer,
      serviceName: String,
      parentSpan: Option[DDSpan] = None,
      correlationId: String = UUID.randomUUID().toString
  )(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F] =
    OpenTracingContext
      .spanResource(tracer, operationName, parentSpan)
      .map(new DDTracingContext(tracer, _, serviceName, correlationId))
      .evalMap(ctx => ctx.addTags(tags + (SERVICE_NAME -> serviceName)).map(_ => ctx))

  private def buildAndRegisterDDTracer[F[_]: Sync] =
    for {
      tracer <- Sync[F].delay(new DDTracer())
      _ <- OpenTracingContext.registerGlobalTracer(tracer)
      _ <- Sync[F].delay {
        _root_.datadog.trace.api.GlobalTracer.registerIfAbsent(tracer)
      }
    } yield tracer

  def getDDTracingContextBuilder[F[_]: Sync](serviceName: String): F[TracingContextBuilder[F]] =
    buildAndRegisterDDTracer.map(tracer => apply(tracer, serviceName))
}
