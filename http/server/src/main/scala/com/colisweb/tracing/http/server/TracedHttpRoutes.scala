package com.colisweb.tracing.http.server

import java.util.UUID

import cats.data._
import cats.effect._
import cats.implicits._
import com.colisweb.tracing.core.{TracingContext, TracingContextBuilder}
import io.opentracing._
import io.opentracing.tag.Tags._
import org.http4s._
import org.http4s.util.CaseInsensitiveString

object TracedHttpRoutes {

  final val correlationIdHeaderName = "X-Correlation-Id"
  type EnrichedRequest[F[_]] = (Request[F], String)

  def enrichRequest[F[_]](request: Request[F]): EnrichedRequest[F] = {
    val idHeader = request.headers.get(CaseInsensitiveString(correlationIdHeaderName))

    val correlationId  = idHeader.fold(UUID.randomUUID.toString)(_.value)
    val enrichedHeader = Header(correlationIdHeaderName, correlationId)

    (request.putHeaders(enrichedHeader), correlationId)
  }

  def apply[F[_]: Sync](
      pf: PartialFunction[TracedRequest[F], F[Response[F]]]
  )(implicit
      builder: TracingContextBuilder[F]
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
      val (enrichedRequest, correlationId) = enrichRequest(req)

      val operationName = "http4s-incoming-request"
      val tags = Map(
        HTTP_METHOD.getKey -> enrichedRequest.method.name,
        HTTP_URL.getKey    -> enrichedRequest.uri.path.toString
      )

      OptionT {
        builder.build(operationName, tags, correlationId) use { context =>
          val tracedRequest = TracedRequest[F](enrichedRequest, context)
          val responseOptionWithTags = routes.run(tracedRequest) semiflatMap { response =>
            val tags = Map(
              HTTP_STATUS.getKey -> response.status.code.toString
            ) ++
              response.headers.toList.map(h => (s"http.response.header.${h.name}" -> h.value)).toMap
            context.addTags(tags).map(_ => response).map(_.putHeaders(Header(correlationIdHeaderName, correlationId)))
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
