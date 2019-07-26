package com.colisweb.tracing

import cats.effect._
import cats.implicits._
import com.colisweb.tracing.TracingContext._
import io.opentracing._

class OpenTracingContext[F[_]: Sync, T <: Tracer, S <: Span](
    tracer: T,
    span: S
) extends TracingContext[F] {

  def childSpan(operationName: String, tags: Map[String, String] = Map.empty): TracingContextResource[F] =
    OpenTracingContext[F, T, S](
      tracer,
      s => Sync[F].pure(new OpenTracingContext[F, T, S](tracer, s)),
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
      liftSpanInContext: S => F[TracingContext[F]],
      parentSpan: Option[S]
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
        ctx  <- liftSpanInContext(span)
        _    <- ctx.addTags(tags)
      } yield ctx
    }

    def release(context: TracingContext[F]): F[Unit] = context.close()

    Resource.make(acquire)(release)
  }
}
