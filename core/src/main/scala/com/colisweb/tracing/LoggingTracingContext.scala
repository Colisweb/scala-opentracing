package com.colisweb.tracing

import cats.effect._
import cats.syntax.all._
import com.colisweb.tracing.TracingContext._
import com.colisweb.tracing.utils._
import java.{util => ju}
import cats.data.OptionT
import scala.concurrent.duration.MILLISECONDS
import com.typesafe.scalalogging.StrictLogging

class LoggingTracingContext[F[_]: Sync: Timer](traceIdP: String, spanIdP: String)
    extends TracingContext[F] {

  override def spanId: OptionT[F, String] = OptionT.pure(spanIdP)
  override def traceId: OptionT[F, String] = OptionT.pure(traceIdP)

  // TODO : store tags somewhere
  def addTags(tags: Map[String, String]): F[Unit] = Sync[F].unit

  def childSpan(
      operationName: String,
      tags: Map[String, String]
  ): com.colisweb.tracing.TracingContext.TracingContextResource[F] =
    LoggingTracingContext(Some(this))(operationName)
}

object LoggingTracingContext extends StrictLogging {

  private case class SpanDetails[F[_]](
      start: Long,
      traceId: String,
      spanId: String,
      ctx: LoggingTracingContext[F]
  )

  def apply[F[_]: Sync: Timer](
      parentContext: Option[LoggingTracingContext[F]] = None,
      idGenerator: Option[F[String]] = None,
      slf4jLogger: org.slf4j.Logger = logger.underlying
  )(
      operationName: String,
      tags: Map[String, String] = Map.empty
  ): TracingContextResource[F] =
    resource(parentContext, idGenerator, slf4jLogger, operationName)
      .evalMap(ctx => ctx.addTags(tags).map(_ => ctx))

  private def resource[F[_]: Sync: Timer](
      parentContext: Option[LoggingTracingContext[F]], 
      idGenerator: Option[F[String]],
      slf4jLogger: org.slf4j.Logger,
      operationName: String,
  ): TracingContextResource[F] = {
    val logger = PureLogger(slf4jLogger)
    val idGeneratorValue: F[String] =
      idGenerator.getOrElse(Sync[F].delay(ju.UUID.randomUUID().toString))
    val traceIdF: F[String] =
      OptionT.fromOption(parentContext).flatMap(_.traceId).getOrElseF(idGeneratorValue)

    val acquire: F[SpanDetails[F]] = for {
      spanId <- idGeneratorValue
      traceId <- traceIdF
      start <- Clock[F].monotonic(MILLISECONDS)
      ctx = new LoggingTracingContext[F](traceId, spanId)
      details = SpanDetails(start, traceId, spanId, ctx)
      _ <- logger.trace("Trace {} Starting Span {} ({})", traceId, spanId, operationName)
    } yield details

    def release(input: SpanDetails[F]): F[Unit] = input match {
      case SpanDetails(start, traceId, spanId, _) =>
        for {
          end <- Clock[F].monotonic(MILLISECONDS)
          duration = end - start
          _ <- logger.trace(
            "Trace {} Finished Span {} ({}) in {}ms",
            traceId,
            spanId,
            operationName,
            duration.toString
          )
        } yield () 
    }

    Resource.make(acquire)(release).map(_.ctx)
  }
}
