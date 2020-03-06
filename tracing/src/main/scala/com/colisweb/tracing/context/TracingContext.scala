package com.colisweb.tracing.context

import cats.Applicative
import cats.data.OptionT
import com.colisweb.tracing.domain.{LoggingContext, Tags}

/**
  * Represents a computational context may have unique `spanId` and `traceId` identifiers and
  * should be able to spawn child context
  */
abstract class TracingContext[F[_]: Applicative] extends LoggingContext[F]{

  /**
    * A trace is a tree of spans. It has a root span than can have many children. This id should be the
    * same across the entire tree
    */
  def traceId: OptionT[F, String] = OptionT.none

  /**
    * A Span is an individual unit of work. Its id should be unique across the entire system
    */
  def spanId: OptionT[F, String] = OptionT.none

  /**
    * This creates a new TracingContext. The trace id should preserved across children, and the children
    * must be closed in the reverse-order of their creation
    */
  def childSpan(operationName: String, tags: Tags = Map.empty): TracingContextResource[F]

  /**
   * A correlation id will be the same for the context and its descendant
   * It is meant to follow the logging path across multiple services
   */
  def correlationId: String
}
