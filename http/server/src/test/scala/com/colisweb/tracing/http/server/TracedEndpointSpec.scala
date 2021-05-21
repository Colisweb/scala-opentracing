package com.colisweb.tracing.http.server

import cats.effect.IO
import com.colisweb.tracing.context.NoOpTracingContext
import com.colisweb.tracing.core.{TracingContext, TracingContextBuilder}
import com.colisweb.tracing.http.server.TracedHttpRoutes._
import org.http4s.Request
import org.http4s.util.CaseInsensitiveString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.tapir.endpoint

import scala.concurrent.ExecutionContext
import cats.effect.Temporal

final class TracedEndpointSpec extends AnyFlatSpec with Matchers {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Temporal[IO]               = IO.timer(ExecutionContext.global)
  implicit val tcb: TracingContextBuilder[IO] = NoOpTracingContext.builder[IO]().unsafeRunSync()

  def dumbLogic: (Unit, TracingContext[IO]) => IO[Either[Unit, Unit]] = (_, _) => IO(Right(()))

  "A response" should "reuse the request's correlation id if it exists" in {
    val (enrichedRequest, correlationId) = enrichRequest(Request[IO]())
    val response                         = endpoint.toTracedRoute[IO](dumbLogic).run(enrichedRequest).value.unsafeRunSync.get

    val correlationHeader     = response.headers.get(CaseInsensitiveString(correlationIdHeaderName))
    val responseCorrelationId = correlationHeader.get.value

    responseCorrelationId should equal(correlationId)
  }

  "A response" should "contain a new correlation id if the request does not contain one" in {
    val request: Request[IO] = Request()

    val response = endpoint.toTracedRoute[IO](dumbLogic).run(request).value.unsafeRunSync.get

    val maybeResponseCorrelationId =
      response.headers.get(CaseInsensitiveString(correlationIdHeaderName))

    maybeResponseCorrelationId should not be empty
  }

}
