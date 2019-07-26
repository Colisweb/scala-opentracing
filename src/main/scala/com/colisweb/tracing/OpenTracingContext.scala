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

  def close(): F[Unit] = Sync[F].delay {
    span.finish()
  }

  def addTags(tags: Map[String, String]): F[Unit] = Sync[F].delay {
    tags.foreach {
      case (key, value) => span.setTag(key, value)
    }
  }
}

object OpenTracingContext {

  def apply[F[_]: Sync, T <: Tracer, S <: Span](
      tracer: T,
      parentSpan: Option[S] = None,
      liftSpanInContext: Option[S => F[TracingContext[F]]] = None,
  )(
      operationName: String,
      tags: Map[String, String] = Map.empty
  ): TracingContextResource[F] = {

    val acquire: F[TracingContext[F]] = {
      val spanBuilder = {
        val span = tracer.buildSpan(operationName)
        val spanWithParent = parentSpan match {
          case Some(s) => span.asChildOf(s)
          case None    => span
        }
        spanWithParent
      }

      for {
        span <- Sync[F].delay { spanBuilder.start().asInstanceOf[S] }
        ctx <- liftSpanInContext match {
          case Some(fn) => fn(span)
          case None     => Sync[F].pure(new OpenTracingContext(tracer, span))
        }
        _ <- ctx.addTags(tags)
      } yield ctx
    }

    def release(context: TracingContext[F]): F[Unit] = context.close()

    Resource.make(acquire)(release)
  }
}
