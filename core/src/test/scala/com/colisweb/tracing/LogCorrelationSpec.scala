package com.colisweb.tracing

import org.scalatest._
import cats.data._
import cats.effect._
import java.util.UUID
import _root_.datadog.opentracing._
import com.typesafe.scalalogging.StrictLogging
import com.colisweb.tracing.implicits._
import TestUtils._
import com.colisweb.tracing.datadog.DDTracingContext

class LogCorrelationSpec extends FunSpec with StrictLogging with Matchers {

  implicit val slf4jLogger: org.slf4j.Logger = logger.underlying

  describe("Datadog log correlation") {
    it("Should log trace id as a JSON field when TracingContext has a trace id") {
      val traceId = UUID.randomUUID().toString()
      val context = mockDDTracingContext(OptionT.none, OptionT.pure(traceId))
      testStdOut(
        context.logger.info("Hello"),
        _ should include(
          s""""dd.trace_id":"$traceId""""
        )
      )
    }

    it("Should log span id as JSON field when TracingContext has a span id") {
      val spanId = UUID.randomUUID().toString()
      val context = mockDDTracingContext(OptionT.pure(spanId), OptionT.none)
      testStdOut(
        context.logger.info("Hello"),
        _ should include(
          s""""dd.span_id":"$spanId""""
        )
      )
    }

    it("Should not add anything when TracingContext has neither a span id nor a trace id") {
      val context = mockDDTracingContext(OptionT.none, OptionT.none)
      testStdOut(
        context.logger.info("Hello"),
        _ should (not include ("trace_id") and not include ("span_id"))
      )
    }
  }

  private def mockDDTracingContext(
      _spanId: OptionT[IO, String],
      _traceId: OptionT[IO, String]
  ) = {
    val tracer = new DDTracer()
    val span: DDSpan = tracer.activeSpan().asInstanceOf[DDSpan]

    new DDTracingContext[IO](tracer, span, "Mocked service") {
      override def childSpan(operationName: String, tags: Tags) = ???
      override def spanId: cats.data.OptionT[cats.effect.IO, String] = _spanId
      override def traceId: cats.data.OptionT[cats.effect.IO, String] = _traceId
      override def addTags(tags: Tags): cats.effect.IO[Unit] = ???
      def close(): cats.effect.IO[Unit] = ???
    }
  }
}
