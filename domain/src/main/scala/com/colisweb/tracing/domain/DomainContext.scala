package com.colisweb.tracing.domain

import org.slf4j.Logger

trait DomainContext[F[_]] {

  def addToMDC(key: String, value: String): Unit

  def logger(implicit slf4jLogger: Logger): PureLogger[F]

}
