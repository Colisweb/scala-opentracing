package com.colisweb.application.context.amqp

import dev.profunktor.fs2rabbit.model._
import fs2.Stream
import org.scalatest.flatspec.AnyFlatSpec

final class AmqpMessageSpec extends AnyFlatSpec {

  final val deliveryTag  = DeliveryTag(0)
  final val payload      = "dumb message"
  final val exchangeName = ExchangeName("")
  final val routingKey   = RoutingKey("")

  final val initialCorrelationId = "30c3ed83-45be-4cd8-acb8-5f1a04fdb3dd"

  final val emptyProperties             = AmqpProperties()
  final val propertiesWithCorrelationId = AmqpProperties(correlationId = Some(initialCorrelationId))

  "a stream" should "add a correlation id to messages that do not have one" in {
    val envelopeWithoutCorrelationId = AmqpEnvelope(
      deliveryTag = deliveryTag,
      payload = payload,
      properties = emptyProperties,
      exchangeName = exchangeName,
      routingKey = RoutingKey(""),
      redelivered = false
    )

    val envelopeWithCorrelationId = AmqpEnvelope(
      deliveryTag = deliveryTag,
      payload = payload,
      properties = propertiesWithCorrelationId,
      exchangeName = exchangeName,
      routingKey = RoutingKey(""),
      redelivered = false
    )

    val stream = Stream(envelopeWithoutCorrelationId, envelopeWithCorrelationId).withCorrelationId

    stream.compile.toList.flatMap(_.properties.correlationId) match {
      case List(_, _) => succeed
      case _          => fail
    }
  }

}
