package com.colisweb.tracing

import cats.data.OptionT
import cats.effect._

abstract class TracingContext[F[_]: Sync] {
  import TracingContext._

  def traceId: OptionT[F, String] = OptionT.none
  def spanId: OptionT[F, String]  = OptionT.none

  def close(): F[Unit]
  def addTags(tags: Map[String, String]): F[Unit]
  def childSpan(operationName: String, tags: Map[String, String] = Map.empty): TracingContextResource[F]
}

object TracingContext {
  type TracingContextResource[F[_]] = Resource[F, TracingContext[F]]
  trait TracingContextBuilder[F[_]] {
    def apply(operationName: String, tags: Map[String, String]): TracingContextResource[F]
  }
}
