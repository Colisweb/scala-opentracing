package com.colisweb.tracing.http.server

import cats.data._
import cats.effect._
import com.colisweb.tracing.core.{TracingContext, TracingContextBuilder}
import org.http4s._
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter.{toRouteRecoverErrors, toRoutes}
import sttp.tapir.server.http4s._

import scala.reflect.ClassTag
import cats.effect.Temporal

trait TracedRoutes {

  implicit class TracedEndpoint[In, Err, Out](e: Endpoint[In, Err, Out, Any]) {

    def toTracedRoute[F[_]: Sync: Concurrent: Temporal](logic: (In, TracingContext[F]) => F[Either[Err, Out]])(implicit
        builder: TracingContextBuilder[F],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] = {

      TracedHttpRoutes.wrapHttpRoutes(
        Kleisli[OptionT[F, *], TracedRequest[F], Response[F]] { req =>
          toRoutes(e)(input => logic(input, req.tracingContext))(
            serverOptions,
            implicitly,
            cs,
            implicitly
          ).run(req.request)
        },
        builder
      )
    }
  }

  implicit class TracedEndpointRecoverErrors[In, Err <: Throwable, Out](
      e: Endpoint[In, Err, Out, Any]
  ) {
    def toTracedRouteRecoverErrors[F[_]: Sync: Concurrent: Temporal](logic: (In, TracingContext[F]) => F[Out])(implicit
        builder: TracingContextBuilder[F],
        eClassTag: ClassTag[Err],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] =
      TracedHttpRoutes.wrapHttpRoutes(
        Kleisli[OptionT[F, *], TracedRequest[F], Response[F]] { req =>
          toRouteRecoverErrors(e)(input => logic(input, req.tracingContext))(
            serverOptions,
            implicitly,
            implicitly,
            implicitly,
            implicitly,
            implicitly
          ).run(req.request)
        },
        builder
      )
  }
}
