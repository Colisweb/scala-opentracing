package com.colisweb.tracing.http4s

import cats.data._
import cats.effect._
import cats.implicits._
import com.colisweb.tracing.TracingContext
import com.colisweb.tracing.TracingContext._
import io.opentracing._
import io.opentracing.tag.Tags._
import org.http4s._

object TracedHttpRoutes {

  def apply[F[_]: Sync](
      pf: PartialFunction[TracedRequest[F], F[Response[F]]]
  )(
      implicit builder: TracingContextBuilder[F]
  ): HttpRoutes[F] = {
    // This could be avoided using the kind projector plugin
    type KleisliF[A] = OptionT[F, A]
    Kleisli[KleisliF, Request[F], Response[F]] { req =>
      val operationName = "http4s-incoming-request"
      val tags = Map(
        HTTP_METHOD.getKey -> req.method.name,
        HTTP_URL.getKey    -> req.uri.path.toString
      )

      OptionT {
        builder(operationName, tags) use { context =>
          val tracedRequest = TracedRequest[F](req, context)
          val responseOption: OptionT[F, Response[F]] =
            pf.andThen(OptionT.liftF(_)).applyOrElse(tracedRequest, Function.const(OptionT.none))
          val responseOptionWithTags = responseOption semiflatMap { response =>
            val tags = Map(
              HTTP_STATUS.getKey() -> response.status.code.toString
            ) ++
              response.headers.toList.map(h => (s"http.response.header.${h.name}" -> h.value)).toMap
            context
              .addTags(tags)
              .map(_ => response)
          }
          responseOptionWithTags.value
        }
      }
    }
  }

  object using {
    def unapply[F[_], T <: Tracer, S <: Span](
        tr: TracedRequest[F]
    ): Option[(Request[F], TracingContext[F])] =
      Some(tr.request -> tr.tracingContext)
  }
}
