package com.colisweb.application.context.amqp

import java.util.UUID

import dev.profunktor.fs2rabbit.model.AmqpEnvelope
import fs2.Stream

trait AmqpConsumerWithCorrelationIdHelper {

  implicit final class AmqpConsumerWithCorrelationId[F[_], T](stream: Stream[F, AmqpEnvelope[T]]) {

    def withCorrelationId: Stream[F, AmqpEnvelope[T]] = stream.map(enrichWithCorrelationId)

    private def enrichWithCorrelationId(envelope: AmqpEnvelope[T]): AmqpEnvelope[T] = {
      val properties       = envelope.properties
      val correlationId    = properties.correlationId.orElse(Some(UUID.randomUUID.toString))
      val propertiesWithId = properties.copy(correlationId = correlationId)
      envelope.copy(properties = propertiesWithId)
    }
  }
}
