package com.colisweb.tracing.cats.instrumented

import cats.effect.{Bracket, ExitCase}
import io.opentracing.Tracer
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.util.GlobalTracer

trait Tracing[F[_]] {

  private val tracer: Tracer = GlobalTracer.get()

  protected def span[A](name: String, serviceName: String)(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] =
    span(name, serviceName, identity[SpanBuilder](_))(computation)

  protected def span[A](name: String, serviceName: String, customize: SpanBuilder => SpanBuilder)(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] =
    span(name, builder => customize(builder.withTag("service", serviceName)))(computation)

  protected def rootSpan[A](name: String, serviceName: String)(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] =
    rootSpan(name, serviceName, identity[SpanBuilder](_))(computation)

  protected def rootSpan[A](name: String, serviceName: String, customize: SpanBuilder => SpanBuilder)(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] =
    rootSpan(name, builder => customize(builder.withTag("service", serviceName)))(computation)

  protected def rootSpan[A](name: String, customize: SpanBuilder => SpanBuilder = identity[SpanBuilder](_))(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] =
    span(name, customize.andThen(_.ignoreActiveSpan()))(computation)

  protected def span[A](name: String, customize: SpanBuilder => SpanBuilder = identity[SpanBuilder](_))(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] = {
    F.bracketCase(
      F.catchNonFatal(
        customize(tracer.buildSpan(name)).start()
      )
    ) { currentSpan =>
      tracer.scopeManager().activate(currentSpan)
      computation
    } { case (currentSpan, result) =>
      result match {
        case ExitCase.Error(error) => currentSpan.log(error.getMessage)
        case ExitCase.Canceled => currentSpan.log("Canceled")
        case _ => ()
      }
      currentSpan.finish()
      F.unit
    }
  }
}
