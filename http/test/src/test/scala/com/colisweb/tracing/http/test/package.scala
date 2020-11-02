package com.colisweb.tracing.http

import java.net.ServerSocket

import cats.effect.{IO, Resource, Sync}
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe._
import sttp.tapir.Codec.JsonCodec
import sttp.tapir._
import sttp.tapir.json.circe._

package object test {

  implicit def responseWithCorrelationIdEntityDecoder[F[_]: Sync]: EntityDecoder[F, WrappedCorrelationId] =
    jsonOf[F, WrappedCorrelationId]

  implicit val responseWithCorrelationIdCodec: JsonCodec[WrappedCorrelationId] =
    circeCodec[WrappedCorrelationId]

  def greetEndpointDefinition: Endpoint[Unit, Unit, WrappedCorrelationId, Nothing] =
    endpoint.get.in("pass_the_weed").out(jsonBody[WrappedCorrelationId])

  def freePort: Int =
    Resource.fromAutoCloseable(IO(new ServerSocket(0))).use(serverSocket => IO(serverSocket.getLocalPort)).unsafeRunSync()

}
