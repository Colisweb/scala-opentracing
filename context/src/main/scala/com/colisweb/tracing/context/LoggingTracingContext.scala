package com.colisweb.tracing.context

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.colisweb.tracing.core._
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.Logger

import scala.concurrent.duration.MILLISECONDS
import cats.effect.{ Ref, Temporal }

/** A tracing context that will log the beginning and the end of all traces along with
  * their tags.
  * The traces will be emitted with a TRACE level, so make sure to configure your logging backend
  * to ennable the TRACE level for com.colisweb.tracing
  */
class LoggingTracingContext[F[_]: Sync: Temporal](
    traceIdP: String,
    spanIdP: String,
    tagsRef: Ref[F, Tags],
    override val correlationId: String
) extends TracingContext[F] {

  def spanId: OptionT[F, String]  = OptionT.pure(spanIdP)
  def traceId: OptionT[F, String] = OptionT.pure(traceIdP)

  def addTags(tags: Tags): F[Unit] = tagsRef.update(_ ++ tags)

  def span(
      operationName: String,
      tags: Tags
  ): TracingContextResource[F] =
    LoggingTracingContext(Some(this), correlationId = correlationId)(operationName)

  override def logger(implicit slf4jLogger: Logger): PureLogger[F] = PureLogger[F](slf4jLogger)
}

object LoggingTracingContext extends StrictLogging {

  /** Returns a Resource[F, TracingContext[F]]. The first log will be emitted
    * as the resource is acquired, the second log when it is released.
    */
  def apply[F[_]: Sync: Temporal](
      parentContext: Option[LoggingTracingContext[F]] = None,
      idGenerator: Option[F[String]] = None,
      slf4jLogger: org.slf4j.Logger = logger.underlying,
      correlationId: String
  )(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F] =
    resource(parentContext, idGenerator, slf4jLogger, operationName, correlationId).evalMap(ctx => ctx.addTags(tags).map(_ => ctx))

  private def resource[F[_]: Sync: Temporal](
      parentContext: Option[LoggingTracingContext[F]],
      idGenerator: Option[F[String]],
      slf4jLogger: org.slf4j.Logger,
      operationName: String,
      correlationId: String
  ): TracingContextResource[F] = {
    val logger                      = PureLogger(slf4jLogger)
    val idGeneratorValue: F[String] = idGenerator.getOrElse(randomUUIDGenerator)
    val traceIdF: F[String] =
      OptionT.fromOption(parentContext).flatMap(_.traceId).getOrElseF(idGeneratorValue)

    val acquire: F[SpanDetails[F]] = for {
      tagsRef <- Ref[F].of[Tags](Map.empty)
      spanId  <- idGeneratorValue
      traceId <- traceIdF
      start   <- Clock[F].monotonic(MILLISECONDS)
      ctx     = new LoggingTracingContext[F](traceId, spanId, tagsRef, correlationId)
      details = SpanDetails(start, traceId, spanId, ctx, tagsRef)
      _ <- logger.trace("Trace {} Starting Span {} ({})", traceId, spanId, operationName)
    } yield details

    def release(input: SpanDetails[F]): F[Unit] = input match {
      case SpanDetails(start, traceId, spanId, _, tagsRef) =>
        for {
          tags <- tagsRef.get
          end  <- Clock[F].monotonic(MILLISECONDS)
          duration = end - start
          _ <- logger.trace(
            "Trace {} Finished Span {} ({}) in {}ms. Tags: {}",
            traceId,
            spanId,
            operationName,
            duration.toString,
            tags.toString
          )
        } yield ()
    }

    Resource.make(acquire)(release).map(_.ctx)
  }

  private def randomUUIDGenerator[F[_]: Sync] = Sync[F].delay(java.util.UUID.randomUUID().toString)

  private case class SpanDetails[F[_]](
      start: Long,
      traceId: String,
      spanId: String,
      ctx: LoggingTracingContext[F],
      tagsRef: Ref[F, Tags]
  )

  /** Returns a F[TracingContextBuilder[F]]
    *
    * This is provided for convenience and consistency with regards to the other
    * tracing contexts types.
    */
  def builder[F[_]: Sync: Temporal]: F[TracingContextBuilder[F]] =
    Sync[F].delay((operationName: String, tags: Tags, correlationId: String) =>
      LoggingTracingContext.apply(correlationId = correlationId)(operationName, tags)
    )
}
