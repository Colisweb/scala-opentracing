package com.colisweb.tracing.http.server

import java.util.UUID

import cats.data.OptionT
import cats.effect.{IO, Resource}
import com.colisweb.tracing.core._
import org.http4s.Request
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.Logger
import sttp.tapir.Codec.string
import sttp.tapir._
import sttp.tapir.generic.auto._

import scala.concurrent.ExecutionContext
import cats.effect.{ Deferred, Temporal }

class TapirSpec extends AsyncFunSpec with Matchers {
  implicit val timer : Temporal[IO] = IO.timer(ExecutionContext.global)
  import TapirSpec._

  describe("Tapir Integration") {
    it("Should create a tracing context and pass it to the logic function") {
      (for {
        tracingContextDeferred <- Deferred[IO, TracingContext[IO]]
        _ <- myEndpoint
          .toTracedRoute[IO]((_, ctx: TracingContext[IO]) => tracingContextDeferred.complete(ctx) *> IO.pure(Right("Ok")))
          .run(request)
          .value
        tracingContext <- tracingContextDeferred.get
        receivedTracingContext = tracingContext.asInstanceOf[MockedTracingContext]
      } yield {
        receivedTracingContext.traceId shouldBe mockedContext.traceId
        receivedTracingContext.spanId shouldBe mockedContext.spanId
      }).unsafeToFuture()
    }

    it("Should serve a the correct response when the endpoint is called with a valid request") {
      val output = java.util.UUID.randomUUID().toString
      myEndpoint
        .toTracedRoute[IO]((_, _) => IO.pure(Right(output)))
        .run(request)
        .value
        .map(_.get)
        .flatMap(_.as[String])
        .map(res => res shouldBe output)
        .unsafeToFuture()
    }

    it("Should serve an error when an exception is thrown from the endpoint logic") {
      val endpointWithError = myEndpoint.errorOut(plainBody[EndpointError](endpointErrorCodec))
      endpointWithError
        .toTracedRouteRecoverErrors[IO]((_, _) => IO.raiseError(EndpointError("Something terrible happened")))
        .run(request)
        .value
        .map(_.get)
        .flatMap(_.as[String])
        .map(res => res shouldBe "Message: Something terrible happened")
        .unsafeToFuture()
    }
  }

}
object TapirSpec {
  implicit def endpointErrorCodec: Codec[String, EndpointError, CodecFormat.TextPlain] =
    string.map(EndpointError)(error => s"Message: ${error.message}").schema(implicitly[Schema[EndpointError]])

  implicit def mockedContextBuilder: TracingContextBuilder[IO] =
    (_, _, _) => Resource.pure[IO, TracingContext[IO]](mockedContext)

  implicit def cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  case class EndpointError(message: String) extends RuntimeException

  val myEndpoint: Endpoint[Unit, Unit, String, Any] =
    endpoint.get.in("").name("My endpoint").out(stringBody)

  val request: Request[IO] = Request[IO]()

  val randomSpanId: String = java.util.UUID.randomUUID().toString

  val mockedContext = new MockedTracingContext

  class MockedTracingContext extends TracingContext[IO] {

    def spanId: OptionT[IO, String]                                   = OptionT.pure(randomSpanId)
    def traceId: OptionT[IO, String]                                  = OptionT.pure(randomSpanId)
    override def correlationId: String                                = UUID.randomUUID().toString
    override def logger(implicit slf4jLogger: Logger): PureLogger[IO] = PureLogger(slf4jLogger)

    def addTags(tags: Tags): cats.effect.IO[Unit] = IO.unit
    def span(
        operationName: String,
        tags: Tags
    ): TracingContextResource[cats.effect.IO] = ???
  }

}
