package com.colisweb.tracing.implicits

import cats.effect._
import cats.data._

trait Syntax {
  implicit class ResourceOps[F[_]: Sync, A](resource: Resource[F, A]) {

    /** Allows ignoring the value of a Resource.
      * {{{
      * import com.colisweb.tracing.implicits._
      *
      * someTracingContext.childContext("Child operation") wrap F.delay { /* Some computation */ }
      * }}}
      *
      * is equivalent to
      * {{{
      * someTracingContext.childContext("Child operation") use { _ => F.delay { /* Some computation */ } }
      * }}}
      */
    def wrap[B](body: => F[B]): F[B] = resource.use(_ => body)

    /** Allows allocating a Resource and supplying it to a function returning
      * an EitherT.
      */
    def either[L, R](body: A => EitherT[F, L, R]): EitherT[F, L, R] =
      EitherT(resource.use(a => body(a).value))

    /** Allows allocating a Resource and supplying it to a function returning
      * an OptionT.
      */
    def option[B](body: A => OptionT[F, B]): OptionT[F, B] =
      OptionT(resource.use(a => body(a).value))
  }
}