package com.colisweb.tracing.http.server

import cats.implicits._
import cats.data._
import cats.effect._
import com.colisweb.tracing.core.{TracingContext, TracingContextBuilder}
import org.http4s._
import org.slf4j.{Logger, LoggerFactory}
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s._

import scala.reflect.ClassTag

trait TracedRoutes {

  implicit class TracedEndpoint[In, Err, Out](e: Endpoint[In, Err, Out, Nothing]) {

    implicit val logger: Logger = LoggerFactory.getLogger("endpoint." + e.info.name.getOrElse(""))

    def toTracedRoute[F[_]: Sync](logic: (In, TracingContext[F]) => F[Either[Err, Out]])(
        implicit
        builder: TracingContextBuilder[F],
        cs: ContextShift[F],
        serverOptions: Http4sServerOptions[F]): HttpRoutes[F] = {

      TracedHttpRoutes.wrapHttpRoutes(
        LoggedHttpRoutes.wrapHttpRoutes(Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
          e.toRoutes(input => logic(input, req.tracingContext))(
              serverOptions,
              implicitly,
              cs
            )
            .run(req.request)
        }),
        builder
      )
    }
  }

  implicit class TracedEndpointRecoverErrors[In, Err <: Throwable, Out](
      e: Endpoint[In, Err, Out, Nothing]
  ) {
    implicit val logger: Logger = LoggerFactory.getLogger("endpoint." + e.info.name.getOrElse(""))

    // TracedHttpRoutes
    private def tracingLogicWrapper[F[_]: Sync](req: Request[F], logic: (In, TracingContext[F]) => F[Out]): In => F[Out] = ???

    // LoggedHttpRoutes
    private def loggingLogicWrapper[F[_]: Sync](req: Request[F], logic: (In, TracingContext[F]) => F[Out]): (In, TracingContext[F]) => F[Out] = ???
    private def logBadRequest[F[_]: Sync](req: Request[F], res: Response[F]): F[Unit] = ???


    def toTracedRouteRecoverErrors[F[_]: Sync](logic: (In, TracingContext[F]) => F[Out])(
        implicit
        builder: TracingContextBuilder[F],
        eClassTag: ClassTag[Err],
        cs: ContextShift[F],
        serverOptions: Http4sServerOptions[F]): HttpRoutes[F] =
      Kleisli[OptionT[F, ?], Request[F], Response[F]] { req =>
        e.toRouteRecoverErrors(
          tracingLogicWrapper(req,
            loggingLogicWrapper(req, logic)
          )
        )(serverOptions, implicitly, implicitly, implicitly, implicitly).run(req).flatTransform {
          case Some(res) if res.status == Status.BadRequest =>
            logBadRequest(req, res) *> Sync[F].pure[Option[Response[F]]](Some(res))
          case other => Sync[F].pure[Option[Response[F]]](other)
        }
//
//      TracedHttpRoutes.wrapHttpRoutes(
//        LoggedHttpRoutes.wrapHttpRoutes(Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
//          e.toRouteRecoverErrors(input => logic(input, req.tracingContext))(
//              serverOptions,
//              implicitly,
//              implicitly,
//              implicitly,
//              implicitly
//            )
//            .run(req.request)
//        }),
//        builder
//      )
    }
  }
}
