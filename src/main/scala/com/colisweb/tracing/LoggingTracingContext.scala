package com.colisweb.tracing

import cats.effect._
import cats.syntax.all._
import com.colisweb.tracing.TracingContext._
import com.colisweb.tracing.utils._
import java.{util => ju}
import cats.data.OptionT
import scala.concurrent.duration.MILLISECONDS
import com.typesafe.scalalogging.StrictLogging

class LoggingTracingContext[F[_]: Sync](spanIdP: String, traceIdP: String)
    extends TracingContext[F] {

  override def spanId: OptionT[F, String] = OptionT.pure(spanIdP)
  override def traceId: OptionT[F, String] = OptionT.pure(traceIdP)

  def addTags(tags: Map[String, String]): F[Unit] = ???
  def childSpan(
      operationName: String,
      tags: Map[String, String]
  ): com.colisweb.tracing.TracingContext.TracingContextResource[F] = ???
}

object LoggingTracingContext extends StrictLogging {

  def apply[F[_]: Sync: Clock](
      operationName: String,
      parentContext: Option[LoggingTracingContext[F]] = None,
      idGenerator: Option[F[String]] = None,
      slf4jLogger: org.slf4j.Logger = logger.underlying
  ): TracingContextResource[F] = {
    val logger = PureLogger(slf4jLogger)
    val idGeneratorValue: F[String] = idGenerator.getOrElse(Sync[F].delay(ju.UUID.randomUUID().toString))
    val traceIdF: F[String] = OptionT.fromOption(parentContext).flatMap(_.traceId).getOrElseF(idGeneratorValue)
    
    val acquire: F[(TracingContext[F], Long)] = for {
      spanId <- idGeneratorValue
      traceId <- traceIdF
      start <- Clock[F].monotonic(MILLISECONDS)
      ctx = new LoggingTracingContext[F](spanId, traceId)
    } yield (ctx, start)

    def release(input: (TracingContext[F], Long)): F[Unit] = input match {
      case (ctx, start) =>
        for {
          end <- Clock[F].monotonic(MILLISECONDS)
        } yield ()
    }
    
    Resource.make(acquire)(release).map(_._1)
  }
}
