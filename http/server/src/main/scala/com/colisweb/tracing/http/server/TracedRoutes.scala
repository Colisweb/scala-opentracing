package com.colisweb.tracing.http.server

import cats.data._
import cats.effect._
import com.colisweb.tracing.core.{TracingContext, TracingContextBuilder}
import org.http4s._
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s._

import scala.reflect.ClassTag

trait TracedRoutes {

  implicit class TracedEndpoint[In, Err, Out](e: Endpoint[In, Err, Out, Nothing]) {

    def toTracedRoute[F[_]: Sync](logic: (In, TracingContext[F]) => F[Either[Err, Out]])(implicit
        builder: TracingContextBuilder[F],
        cs: ContextShift[F],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] = {

      TracedHttpRoutes.wrapHttpRoutes(
        Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
          e.toRoutes(input => logic(input, req.tracingContext))(
            serverOptions,
            implicitly,
            cs
          ).run(req.request)
        },
        builder
      )
    }
  }

  implicit class TracedEndpointRecoverErrors[In, Err <: Throwable, Out](
      e: Endpoint[In, Err, Out, Nothing]
  ) {
    def toTracedRouteRecoverErrors[F[_]: Sync](logic: (In, TracingContext[F]) => F[Out])(implicit
        builder: TracingContextBuilder[F],
        eClassTag: ClassTag[Err],
        cs: ContextShift[F],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] =
      TracedHttpRoutes.wrapHttpRoutes(
        Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
          e.toRouteRecoverErrors(input => logic(input, req.tracingContext))(
            serverOptions,
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
