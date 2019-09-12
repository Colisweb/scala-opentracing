package com.colisweb.tracing

import cats.data.OptionT
import cats.Applicative

/**
 * Represents a computational context may have unique `spanId` and `traceId` identifiers and
 * should be able to spawn child context
 */
abstract class TracingContext[F[_]: Applicative] {
  /**
   * A trace is a tree of spans. It has a root span than can have many children. This id should be the
   * same across the entire tree
   */
  def traceId: OptionT[F, String] = OptionT.none
  /**
   * A Span is an individual unit of work. Its id should be unique across the entire system
   */
  def spanId: OptionT[F, String]  = OptionT.none

  def addTags(tags: Tags): F[Unit]

  /**
   * This creates a new TracingContext. The trace id should preserved across children, and the children
   * must be closed in the reverse-order of their creation
   */
  def childSpan(operationName: String, tags: Tags = Map.empty): TracingContextResource[F]
}