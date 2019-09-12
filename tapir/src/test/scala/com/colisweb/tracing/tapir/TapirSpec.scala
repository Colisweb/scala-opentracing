package com.colisweb.tracing.tapir

import org.scalatest._
import org.http4s.Request
import tapir._
import com.colisweb.tracing._
import cats.effect._
import cats.effect.concurrent.Deferred
import cats.data.OptionT
import cats.implicits._
import scala.concurrent.ExecutionContext

class TapirSpec extends AsyncFunSpec with Matchers {
  describe("Tapir Integration") {
    it("Should create a tracing context and pass it to the logic function") {
      (for {
        tracingContextDeferred <- Deferred[IO, TracingContext[IO]]
        _ <- myEndpoint.toTracedRoute[IO](
            (_, ctx: TracingContext[IO]) => tracingContextDeferred.complete(ctx) *> IO.pure(Right("Ok"))
          )
          .run(request)
          .value
        receivedTracingContext <- tracingContextDeferred.get
      } yield {
        receivedTracingContext.traceId shouldBe mockedContext.traceId
        receivedTracingContext.spanId shouldBe mockedContext.spanId
      }).unsafeToFuture()
    }

    it("Should serve a the correct response when the endpoint is called with a valid request") {
      val output = java.util.UUID.randomUUID().toString
      myEndpoint.toTracedRoute[IO](
          (_, _) => IO.pure(Right(output))
        )
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
        .toTracedRouteRecoverErrors[IO](
          (_, _) => IO.raiseError(EndpointError("Something terrible happened"))
        )
        .run(request)
        .value
        .map(_.get)
        .flatMap(_.as[String])
        .map(res => res shouldBe "Message: Something terrible happened")
        .unsafeToFuture()
    }
  }

  case class EndpointError(message: String) extends RuntimeException

  implicit def endpointErrorCodec: CodecForOptional[EndpointError, MediaType.TextPlain, String] =
    CodecForOptional.fromCodec(
      Codec.stringPlainCodecUtf8.mapDecode(str => DecodeResult.Value(EndpointError(str)))(
        err => s"Message: ${err.message}"
      )
    )

  val myEndpoint: Endpoint[Unit, Unit, String, Nothing] =
    endpoint.get.in("").name("My endpoint").out(stringBody)

  val request = Request[IO]()

  val randomTraceId = java.util.UUID.randomUUID().toString
  val randomSpanId = java.util.UUID.randomUUID().toString

  val mockedContext: TracingContext[IO] = new TracingContext[IO] {
    override def spanId: OptionT[IO, String] = OptionT.pure(randomSpanId)
    override def traceId: OptionT[IO, String] = OptionT.pure(randomSpanId)
    def addTags(tags: Tags): cats.effect.IO[Unit] = IO.unit
    def childSpan(
        operationName: String,
        tags: Tags
    ): TracingContextResource[cats.effect.IO] = ???
  }

  implicit def mockedContextBuilder: TracingContextBuilder[IO] =
    (_, _) => Resource.pure(mockedContext)

  implicit def cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}
