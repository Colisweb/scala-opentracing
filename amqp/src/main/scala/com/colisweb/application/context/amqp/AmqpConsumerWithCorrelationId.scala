package com.colisweb.application.context.amqp

import java.util.UUID

import dev.profunktor.fs2rabbit.model.{AmqpEnvelope, AmqpProperties}
import fs2.Stream

trait AmqpConsumerWithCorrelationId {

  implicit final class AmqpConsumerWithCorrelationId[F[_], T](stream: Stream[F, AmqpEnvelope[T]]) {

    def withCorrelationId: Stream[F, AmqpEnvelope[T]] = stream.map(addCorrelationId)

    private def addCorrelationId(envelope: AmqpEnvelope[T]): AmqpEnvelope[T] =
      envelope.copy(properties = addCoId(envelope.properties))

    private def addCoId(properties: AmqpProperties): AmqpProperties =
      properties.copy(
        correlationId = properties.correlationId.orElse(Some(UUID.randomUUID.toString))
      )

  }

}
