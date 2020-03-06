package com.colisweb.tracing.context

import cats.effect.Sync
import com.colisweb.tracing.domain.{DomainContext, PureLogger}
import org.slf4j.Logger
import org.slf4j.spi.MDCAdapter

final case class InfrastructureContext[F[_]: Sync](
    tracingContext: TracingContext[F],
    mdc: MDCAdapter,
    correlationId: String
) extends DomainContext[F] {

  override def addToMDC(key: String, value: String): Unit = mdc.put(key, value)

  override def logger(implicit slf4jLogger: Logger): PureLogger[F] = PureLogger(slf4jLogger)

}
