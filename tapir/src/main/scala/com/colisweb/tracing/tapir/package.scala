package com.colisweb.tracing

import _root_.tapir._
import _root_.tapir.server.http4s._
import cats.effect._
import com.colisweb.tracing.http4s.TracedHttpRoutes
import com.colisweb.tracing.TracingContext.TracingContextBuilder
import org.http4s._
import org.http4s.implicits._
import scala.reflect.ClassTag

package object tapir {
  implicit class TracedEndpoint[I, E, O](e: Endpoint[I, E, O, Nothing]) {
    def toTracedRoute[F[_]: Sync](logic: (I, TracingContext[F]) => F[Either[E, O]])(
        implicit builder: TracingContextBuilder[F],
        cs: ContextShift[F]
    ): HttpRoutes[F] = {
      TracedHttpRoutes[F] {
        case req => e.toRoutes(in => logic(in, req.tracingContext)).orNotFound.run(req.request)
      }
    }
  }

  implicit class TracedEndpointRecoverErrors[I, E <: Throwable, O](
      e: Endpoint[I, E, O, Nothing]
  ) {
    def toTracedRouteRecoverErrors[F[_]: Sync](logic: (I, TracingContext[F]) => F[O])(
        implicit builder: TracingContextBuilder[F],
        eClassTag: ClassTag[E],
        cs: ContextShift[F]
    ): HttpRoutes[F] = {
      TracedHttpRoutes[F] {
        case req =>
          e.toRouteRecoverErrors(in => logic(in, req.tracingContext)).orNotFound.run(req.request)
      }
    }

  }
}
