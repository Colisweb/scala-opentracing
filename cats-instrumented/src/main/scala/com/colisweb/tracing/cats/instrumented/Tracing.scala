package com.colisweb.tracing.cats.instrumented

import cats.effect.{Bracket, ExitCase}
import io.opentracing.Tracer
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.util.GlobalTracer

trait Tracing[F[_]] {

  private val tracer: Tracer = GlobalTracer.get()

  protected def span[A](name: String, serviceName: String, isRoot: Boolean = false, customize: SpanBuilder => SpanBuilder = identity[SpanBuilder](_))(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] =
    span(name, isRoot, builder => customize(builder.withTag("service", serviceName)))(computation)

  protected def span[A](name: String, isRoot: Boolean = false, customize: SpanBuilder => SpanBuilder = identity[SpanBuilder](_))(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] =
    makeSpan(name, if (isRoot) customize.compose(_.ignoreActiveSpan()) else customize)(computation)

  private def makeSpan[A](name: String, customize: SpanBuilder => SpanBuilder)(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] = {
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
