package com.colisweb.tracing

import cats.effect._
import cats.implicits._
import com.colisweb.tracing.TracingContext._
import io.opentracing._

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
      tags: Map[String, String] = Map.empty
  ): TracingContextResource[F] =
    OpenTracingContext[F, T, S](
      tracer,
      Some(span)
    )(
      operationName,
      tags
    )

  def addTags(tags: Map[String, String]): F[Unit] = Sync[F].delay {
    tags.foreach {
      case (key, value) => span.setTag(key, value)
    }
  }
}

object OpenTracingContext {

  def apply[F[_]: Sync, T <: Tracer, S <: Span](
      tracer: T,
      parentSpan: Option[S] = None
  )(
      operationName: String,
      tags: Map[String, String] = Map.empty
  ): TracingContextResource[F] =
    spanResource(tracer, operationName, parentSpan)
      .map(new OpenTracingContext(tracer, _))
      .evalMap(ctx => ctx.addTags(tags).map(_ => ctx))

  def spanResource[F[_]: Sync, T <: Tracer, S <: Span](
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
