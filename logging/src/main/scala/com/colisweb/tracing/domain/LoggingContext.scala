package com.colisweb.tracing.domain

import org.slf4j.Logger

trait LoggingContext[F[_]] {

  def addTags(tags: Tags): F[Unit]

  def logger(implicit slf4jLogger: Logger): PureLogger[F]
}
