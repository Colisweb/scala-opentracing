package com.colisweb.tracing

import cats.effect._
import cats.implicits._
import io.opentracing._
import io.opentracing.util.GlobalTracer
import com.typesafe.scalalogging.StrictLogging

/**
  * This is meant to be used with any OpenTracing compatible tracer.
  * For usage with Datadog APM, use DDTracingContext instead
  */
class OpenTracingContext[F[_]: Sync, T <: Tracer, S <: Span](
    tracer: T,
    span: S
) extends TracingContext[F] {

  def childSpan(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F] =
    OpenTracingContext[F, T, S](
      tracer,
      Some(span)
    )(
      operationName,
      tags
    )

  def addTags(tags: Tags): F[Unit] = Sync[F].delay {
    tags.foreach {
      case (key, value) => span.setTag(key, value)
    }
  }
}

object OpenTracingContext extends StrictLogging {

  /**
   * Creates a Resource[F, TracingContext[F]]. The underlying span will
   * be automatically closed when the Resource is released.
   */
  def apply[F[_]: Sync, T <: Tracer, S <: Span](
      tracer: T,
      parentSpan: Option[S] = None
  )(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F] =
    spanResource(tracer, operationName, parentSpan)
      .map(new OpenTracingContext(tracer, _))
      .evalMap(ctx => ctx.addTags(tags).map(_ => ctx))

  /**
   * Registers the tracer as the GlobalTracer and returns a F[TracingContextBuilder[F]].
   * This may be necessary depending on the concrete tracing system you use.
   */
  def getOpenTracingContextBuilder[F[_]: Sync, T <: Tracer, S <: Span](
      tracer: T
  ): F[TracingContextBuilder[F]] =
    registerGlobalTracer(tracer).map(_ => OpenTracingContext(tracer))

  def registerGlobalTracer[F[_]: Sync](tracer: Tracer): F[Unit] = Sync[F].delay {
    if (GlobalTracer.isRegistered()) {
      logger.debug(s"Opentracing GlobalTracer is already registered. Skipping registration.")
    } else {
      GlobalTracer.register(tracer)
    }
  }

  private[tracing] def spanResource[F[_]: Sync, T <: Tracer, S <: Span](
      tracer: T,
      operationName: String,
      parentSpan: Option[S] = None
  ): Resource[F, S] = {
    val acquire: F[S] = {
      val spanBuilder = {
        val span = tracer.buildSpan(operationName)
        val spanWithParent = parentSpan match {
          case Some(s) => span.asChildOf(s)
          case None    => span
        }
        spanWithParent
      }
      Sync[F].delay { spanBuilder.start().asInstanceOf[S] }
    }

    def release(s: S): F[Unit] = Sync[F].delay(s.finish())

    Resource.make(acquire)(release)
  }
}
