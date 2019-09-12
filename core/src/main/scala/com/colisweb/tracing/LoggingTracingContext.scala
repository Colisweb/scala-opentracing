package com.colisweb.tracing

import cats.effect._
import cats.syntax.all._
import com.colisweb.tracing.utils._
import cats.data.OptionT
import scala.concurrent.duration.MILLISECONDS
import com.typesafe.scalalogging.StrictLogging
import cats.effect.concurrent.Ref

/**
 * A tracing context that will log the beginning and the end of all traces along with
 * their tags.
 * The traces will be emitted with a TRACE level, so make sure to configure your logging backend
 * to ennable the TRACE level for com.colisweb.tracing
 */
class LoggingTracingContext[F[_]: Sync: Timer](
    traceIdP: String,
    spanIdP: String,
    tagsRef: Ref[F, Tags]
) extends TracingContext[F] {

  override def spanId: OptionT[F, String] = OptionT.pure(spanIdP)
  override def traceId: OptionT[F, String] = OptionT.pure(traceIdP)

  def addTags(tags: Tags): F[Unit] = tagsRef.update(_ ++ tags)

  def childSpan(
      operationName: String,
      tags: Tags
  ): TracingContextResource[F] =
    LoggingTracingContext(Some(this))(operationName)
}

object LoggingTracingContext extends StrictLogging {

  /**
   * Returns a Resource[F, TracingContext[F]]. The first log will be emitted
   * as the resource is acquired, the second log when it is released.
   */
  def apply[F[_]: Sync: Timer](
      parentContext: Option[LoggingTracingContext[F]] = None,
      idGenerator: Option[F[String]] = None,
      slf4jLogger: org.slf4j.Logger = logger.underlying
  )(
      operationName: String,
      tags: Tags = Map.empty
  ): TracingContextResource[F] =
    resource(parentContext, idGenerator, slf4jLogger, operationName)
      .evalMap(ctx => ctx.addTags(tags).map(_ => ctx))

  /**
   * Returns a F[TracingContextBuilder[F]]
   * 
    * This is provided for convenience and conistency with regards to the other
    * tracing contexts types.
    */
  def getLoggingTracingContextBuilder[F[_]: Sync: Timer]: F[TracingContextBuilder[F]] =
    Sync[F].pure(LoggingTracingContext())

  private def resource[F[_]: Sync: Timer](
      parentContext: Option[LoggingTracingContext[F]],
      idGenerator: Option[F[String]],
      slf4jLogger: org.slf4j.Logger,
      operationName: String
  ): TracingContextResource[F] = {
    val logger = PureLogger(slf4jLogger)
    val idGeneratorValue: F[String] = idGenerator.getOrElse(randomUUIDGenerator)
    val traceIdF: F[String] =
      OptionT.fromOption(parentContext).flatMap(_.traceId).getOrElseF(idGeneratorValue)

    val acquire: F[SpanDetails[F]] = for {
      tagsRef <- Ref[F].of[Tags](Map.empty)
      spanId <- idGeneratorValue
      traceId <- traceIdF
      start <- Clock[F].monotonic(MILLISECONDS)
      ctx = new LoggingTracingContext[F](traceId, spanId, tagsRef)
      details = SpanDetails(start, traceId, spanId, ctx, tagsRef)
      _ <- logger.trace("Trace {} Starting Span {} ({})", traceId, spanId, operationName)
    } yield details

    def release(input: SpanDetails[F]): F[Unit] = input match {
      case SpanDetails(start, traceId, spanId, _, tagsRef) =>
        for {
          tags <- tagsRef.get
          end <- Clock[F].monotonic(MILLISECONDS)
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
}
