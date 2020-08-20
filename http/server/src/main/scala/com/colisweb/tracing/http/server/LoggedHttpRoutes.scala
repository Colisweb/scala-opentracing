package com.colisweb.tracing.http.server

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.implicits._
import com.colisweb.tracing.core.TracingContext
import io.opentracing.tag.Tags.{HTTP_METHOD, HTTP_STATUS, HTTP_URL}
import net.logstash.logback.marker.Markers
import net.logstash.logback.marker.Markers.aggregate
import org.http4s.Response
import org.slf4j.Logger

import scala.jdk.CollectionConverters._

object LoggedHttpRoutes {

  private val headerMarkerName        = "http.headers"

  def wrapHttpRoutes[F[_]: Sync](
      routes: Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]]
  )(implicit logger: Logger): Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] = {
    Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
      OptionT.liftF(logRequest(req)) *>
        routes.run(req).semiflatMap { response =>
          logResponse(response, req.tracingContext).map(_ => response)
        }
    }
  }

  private def logRequest[F[_]: Sync](req: TracedRequest[F])(implicit logger: Logger): F[Unit] = {
    val path       = req.request.uri.path
    val methodName = req.request.method.name

    for {
      body <- req.request.bodyText.compile.string

      tags = Markers.appendEntries(
        Map(
          HTTP_METHOD.getKey -> methodName,
          HTTP_URL.getKey    -> path,
        ).asJava
      )
      headers = Markers.append(headerMarkerName, req.request.headers.toList.map(h => (h.name.toString(), h.value)).toMap.asJava)
      marker  = aggregate(tags, headers)
      _ <- req.tracingContext.logger.info(marker, s"$methodName $path : $body")
    } yield ()
  }

  private def logResponse[F[_]: Sync](response: Response[F], tracingContext: TracingContext[F])(implicit logger: Logger): F[Unit] =
    for {
      body <- response.bodyText.compile.string

      tags = Markers.appendEntries(Map(HTTP_STATUS.getKey -> response.status.code.toString).asJava)

      headers = Markers.append(headerMarkerName, response.headers.toList.map(h => (h.name.toString(), h.value)).toMap.asJava)
      marker  = aggregate(tags, headers)
      _ <- tracingContext.logger.info(marker, s"${response.status.code.toString} ${response.status.reason} : $body")
    } yield ()
}
