package com.colisweb.tracing.http.server

import java.util.UUID

import cats.data.Kleisli
import cats.effect.{ContextShift, Sync}
import com.colisweb.tracing.context.{InfrastructureContext, TracingContext, TracingContextBuilder}
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, HttpRoutes, Request}
import org.slf4j.MDC
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.Http4sServerOptions

import scala.reflect.ClassTag

trait HttpTracedEndpointWithExtendedContext {

  final val correlationIdHeaderName = "X-Correlation-Id"

  def correlationId[F[_]](request: Request[F]): String =
    request.headers.get(CaseInsensitiveString(correlationIdHeaderName)).fold(UUID.randomUUID.toString)(_.value)

  implicit final class HttpTracedEndpointWithExtendedContext[In, Err, Out](endpoint: Endpoint[In, Err, Out, Nothing]) {

    def toRouteWithApplicationContext[F[_]](
        logic: (In, InfrastructureContext[F]) => F[Either[Err, Out]]
    )(
        implicit sync: Sync[F],
        builder: TracingContextBuilder[F],
        cs: ContextShift[F],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] =
      Kleisli { req: Request[F] =>
        val requestCorrelationId = correlationId(req)

        endpoint
          .toTracedRoute(
            (input: In, tracingContext: TracingContext[F]) =>
              logic(
                input,
                InfrastructureContext(
                  tracingContext = tracingContext,
                  correlationId = requestCorrelationId,
                  mdc = MDC.getMDCAdapter
                )
              )
          )(sync, builder, cs, serverOptions)
          .run(req)
          .map(_.putHeaders(Header(correlationIdHeaderName, requestCorrelationId)))
      }

  }

  implicit class HttpEndpointWithApplicationContextRecoverErrors[In, Err <: Throwable, Out](
      endpoint: Endpoint[In, Err, Out, Nothing]
  ) {

    def toRouteWithApplicationContextRecoverErrors[F[_]](
        logic: (In, InfrastructureContext[F]) => F[Out]
    )(
        implicit sync: Sync[F],
        cs: ContextShift[F],
        eClassTag: ClassTag[Err],
        builder: TracingContextBuilder[F],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] =
      Kleisli { req: Request[F] =>
        val requestCorrelationId = correlationId(req)

        endpoint
          .toTracedRouteRecoverErrors(
            (input: In, tracingContext: TracingContext[F]) =>
              logic(
                input,
                InfrastructureContext(
                  tracingContext = tracingContext,
                  correlationId = requestCorrelationId,
                  mdc = MDC.getMDCAdapter
                )
              )
          )(sync, builder, eClassTag, cs, serverOptions)
          .run(req)
          .map(_.putHeaders(Header(correlationIdHeaderName, requestCorrelationId)))
      }

  }

}
