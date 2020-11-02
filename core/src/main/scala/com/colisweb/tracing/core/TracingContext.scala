package com.colisweb.tracing.core

import org.slf4j.Logger

/** Represents a computational context may have unique `spanId` and `traceId` identifiers and
  * should be able to spawn child context
  */
trait TracingContext[F[_]] {

  def addTags(tags: Tags): F[Unit]

  def logger(implicit slf4jLogger: Logger): PureLogger[F]

  /** This creates a new TracingContext. The trace id should preserved across children, and the children
    * must be closed in the reverse-order of their creation
    */
  def span(operationName: String, tags: Tags = Map.empty): TracingContextResource[F]

  /** A correlation id will be the same for the context and its descendant
    * It is meant to follow the logging path across multiple services
    */
  def correlationId: String
}
