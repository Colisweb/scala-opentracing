package com.colisweb.tracing.http.server

import java.util.UUID

import cats.effect.{ContextShift, IO, Resource, Timer}
import com.colisweb.tracing.context.{NoOpTracingContext, TracingContext, TracingContextBuilder}
import com.colisweb.tracing.domain.DomainContext
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Request}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.endpoint

import scala.concurrent.ExecutionContext

final class TracedEndpointSpec extends AnyFlatSpec with Matchers {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val tcb: TracingContextBuilder[IO] = (_, _) =>
    Resource.pure[IO, TracingContext[IO]](NoOpTracingContext())

  def dumbLogic: (Unit, DomainContext[IO]) => IO[Either[Unit, Unit]] = (_, _) => IO(Right(()))

  "A response" should "reuse the request's correlation id if it exists" in {
    val request: Request[IO] =
      Request().putHeaders(Header(correlationIdHeaderName, UUID.randomUUID.toString))

    val response = endpoint
      .toRouteWithApplicationContext[IO](dumbLogic)
      .run(request)
      .value
      .unsafeRunSync
      .get

    val maybeRequestCorrelationId =
      request.headers.get(CaseInsensitiveString(correlationIdHeaderName))
    val maybeResponseCorrelationId =
      response.headers.get(CaseInsensitiveString(correlationIdHeaderName))

    maybeResponseCorrelationId should equal(maybeRequestCorrelationId)
  }

  "A response" should "contain a new correlation id if the request does not contain one" in {
    val request: Request[IO] = Request()

    val response = endpoint
      .toRouteWithApplicationContext[IO](dumbLogic)
      .run(request)
      .value
      .unsafeRunSync
      .get

    val maybeResponseCorrelationId =
      response.headers.get(CaseInsensitiveString(correlationIdHeaderName))

    maybeResponseCorrelationId should not be empty
  }

}
