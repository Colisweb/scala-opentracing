package com.colisweb.tracing.http.test

import cats.Applicative
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Sync, Timer}
import cats.implicits._
import com.colisweb.tracing.context.NoOpTracingContext
import com.colisweb.tracing.core.TracingContextBuilder
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpApp, Request, Uri}

import scala.concurrent.ExecutionContext

object TestServer {

  def create[F[_]: ConcurrentEffect: Timer: ContextShift](
      serverPort: Int,
      server2Port: Int
  ): Resource[F, Server[F]] =
    for {
      tracingContextBuilder <- Resource.liftF(NoOpTracingContext.builder())
      exCtx = ExecutionContext.global
      client <- BlazeClientBuilder[F](exCtx).resource
      service = new ServerService(client, server2Port)
      server <- BlazeServerBuilder.apply(exCtx)
        .bindLocal(serverPort)
        .withHttpApp(service.routes(tracingContextBuilder))
        .resource
    } yield server

}

final class ServerService[F[_]: Sync: ContextShift: Timer: ConcurrentEffect](
    client: Client[F],
    server2Port: Int
) {

  import com.colisweb.tracing.http.client._
  import com.colisweb.tracing.http.server._
  import org.http4s.Method._

  final val F = Applicative[F]

  def routes(implicit tracingContextBuilder: TracingContextBuilder[F]): HttpApp[F] =
    greetEndpointDefinition
      .toTracedRoute[F] { (_, context) =>
        val server2GreetingsEndpoint: Uri =
          Uri.unsafeFromString(s"http://localhost:$server2Port/where_the_weed_at")
        client.fetch(
          Request[F](method = GET, uri = server2GreetingsEndpoint).withCorrelationId(context)
        )(response => response.as[WrappedCorrelationId].map(Right(_)))
      }
      .orNotFound

}
