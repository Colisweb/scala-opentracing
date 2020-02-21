package com.colisweb.tracing

import org.scalatest._
import TestUtils._
import cats.effect.IO
import scala.concurrent.duration._
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class LoggingTracingContextSpec extends AnyFunSpec with Matchers with StrictLogging {
  describe("LoggingTracingContext") {

    it("Should log at the start and the end on the operation") {

      val operationName = "My Operation"
      val context = LoggingTracingContext[IO]()(operationName)
      val operation: IO[TracingContext[IO]] = context use { ctx =>
        IO.sleep(100 millis).map(_ => ctx)
      }

      testStdOut[TracingContext[IO]](
        operation,
        (stdOut, ctx) => {
          val traceId: String = ctx.traceId.value.unsafeRunSync().get
          val spanId: String = ctx.spanId.value.unsafeRunSync().get
          stdOut should (
            include(s"Trace ${traceId} Starting Span ${spanId} ($operationName)")
              and include(s"Finished Span ${spanId}")
          )
        }
      )
    }

    it("Should preserve Trace ids across child contexts") {
      (LoggingTracingContext[IO]()("Parent") use { parentCtx =>
        parentCtx.childSpan("Child context") use { childContext =>
          IO {
            parentCtx.traceId.value.unsafeRunSync() shouldBe childContext.traceId.value
              .unsafeRunSync()
            parentCtx.spanId.value.unsafeRunSync() shouldNot be(
              childContext.spanId.value.unsafeRunSync()
            )
          }
        }
      }).unsafeRunSync()
    }
  }
}
