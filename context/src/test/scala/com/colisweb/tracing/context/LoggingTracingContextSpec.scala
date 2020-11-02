package com.colisweb.tracing.context

import cats.effect.IO
import TestUtils._
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class LoggingTracingContextSpec extends AnyFunSpec with Matchers with StrictLogging {
  describe("LoggingTracingContext") {

    it("Should log at the start and the end on the operation") {

      val operationName = "My Operation"
      val context       = LoggingTracingContext[IO](correlationId = "")(operationName)
      val operation: IO[LoggingTracingContext[IO]] = context use { ctx =>
        IO.sleep(100 millis).map(_ => ctx.asInstanceOf[LoggingTracingContext[IO]])
      }

      testStdOut[LoggingTracingContext[IO]](
        operation,
        (stdOut, ctx) => {
          val traceId: String = ctx.traceId.value.unsafeRunSync().get
          val spanId: String  = ctx.spanId.value.unsafeRunSync().get
          stdOut should (
            include(s"Trace ${traceId} Starting Span ${spanId} ($operationName)")
              and include(s"Finished Span ${spanId}")
          )
        }
      )
    }

    it("Should preserve Trace ids across child contexts") {
      (LoggingTracingContext[IO](correlationId = "")("Parent") use { parent =>
        val parentCtx = parent.asInstanceOf[LoggingTracingContext[IO]]
        parentCtx.span("Child context") use { child =>
          val childCtx = child.asInstanceOf[LoggingTracingContext[IO]]
          IO {
            parentCtx.traceId.value.unsafeRunSync() shouldBe childCtx.traceId.value.unsafeRunSync()
            parentCtx.spanId.value.unsafeRunSync() shouldNot be(
              childCtx.spanId.value.unsafeRunSync()
            )
          }
        }
      }).unsafeRunSync()
    }
  }
}
