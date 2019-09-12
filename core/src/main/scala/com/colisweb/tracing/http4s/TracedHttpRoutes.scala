package com.colisweb.tracing.http4s

import cats.data._
import cats.effect._
import cats.implicits._
import com.colisweb.tracing._
import io.opentracing._
import io.opentracing.tag.Tags._
import org.http4s._

object TracedHttpRoutes {

  def apply[F[_]: Sync](
      pf: PartialFunction[TracedRequest[F], F[Response[F]]]
  )(
      implicit builder: TracingContextBuilder[F]
  ): HttpRoutes[F] = {
    val tracedRoutes = Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
      pf.andThen(OptionT.liftF(_)).applyOrElse(req, Function.const(OptionT.none))
    }
    wrapHttpRoutes(tracedRoutes, builder)
  }

  def wrapHttpRoutes[F[_]: Sync](
      routes: Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]],
      builder: TracingContextBuilder[F]
  ): HttpRoutes[F] = {
    Kleisli[OptionT[F, ?], Request[F], Response[F]] { req =>
      val operationName = "http4s-incoming-request"
      val tags = Map(
        HTTP_METHOD.getKey -> req.method.name,
        HTTP_URL.getKey -> req.uri.path.toString
      )

      OptionT {
        builder(operationName, tags) use { context =>
          val tracedRequest = TracedRequest[F](req, context)
          val responseOptionWithTags = routes.run(tracedRequest) semiflatMap { response =>
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
