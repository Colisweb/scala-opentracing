package com.colisweb.tracing.context

import cats.effect._
import cats.implicits._
import com.colisweb.tracing.core._
import com.typesafe.scalalogging.StrictLogging
import io.opentracing._
import io.opentracing.util.GlobalTracer
import org.slf4j.Logger

/**
  * This is meant to be used with any OpenTracing compatible tracer.
  * For usage with Datadog APM, use DDTracingContext instead
  */
class OpenTracingContext[F[_]: Sync, T <: Tracer, S <: Span](
    tracer: T,
    span: S,
    override val correlationId: String
) extends TracingContext[F] {

  def span(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F] =
    OpenTracingContext[F, T, S](
      tracer,
      Some(span),
      correlationId
    )(
      operationName,
      tags
    )

  def addTags(tags: Tags): F[Unit] = Sync[F].delay {
    tags.foreach {
      case (key, value) => span.setTag(key, value)
    }
  }

  override def logger(implicit slf4jLogger: Logger): PureLogger[F] = PureLogger[F](slf4jLogger)
}

object OpenTracingContext extends StrictLogging {

  /**
    * Creates a Resource[F, TracingContext[F]]. The underlying span will
    * be automatically closed when the Resource is released.
    */
  def apply[F[_]: Sync, T <: Tracer, S <: Span](
      tracer: T,
      parentSpan: Option[S] = None,
      correlationId: String
  )(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F] =
    spanResource(tracer, operationName, parentSpan)
      .map(new OpenTracingContext(tracer, _, correlationId))
      .evalMap(ctx => ctx.addTags(tags).map(_ => ctx))

  /**
    * Registers the tracer as the GlobalTracer and returns a F[TracingContextBuilder[F]].
    * This may be necessary depending on the concrete tracing system you use.
    */
  def builder[F[_]: Sync, T <: Tracer, S <: Span](tracer: T): F[TracingContextBuilder[F]] = {
    for {
      _ <- Sync[F].delay(GlobalTracer.registerIfAbsent(tracer))
    } yield
      new TracingContextBuilder[F] {
        override def build(operationName: String, tags: Tags, correlationId: String): TracingContextResource[F] =
          OpenTracingContext(tracer, correlationId = correlationId)(operationName, tags)
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
