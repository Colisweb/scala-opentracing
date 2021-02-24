package com.colisweb.tracing.cats.instrumented

import cats.effect.{Bracket, ExitCase}
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer

trait Tracing[F[_]] {
  val serviceName: String

  private val tracer: Tracer = GlobalTracer.get()

  protected def span[A](name: String)(computation: => F[A])(implicit F: Bracket[F, Throwable]): F[A] = {
    F.bracketCase(
      F.catchNonFatal(tracer.buildSpan(name).withTag("service", serviceName).start())
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
