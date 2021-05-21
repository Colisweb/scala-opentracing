package com.colisweb.tracing.context

import scala.concurrent._
import scala.concurrent.duration._
import org.scalatest._
import cats.effect._
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import cats.effect.Temporal

object TestUtils {
  implicit val timer: Temporal[IO] = IO.timer(ExecutionContext.global)

  def testStdOut[A](
      body: IO[A],
      assertion: (String, A) => Assertion
  ): Assertion = {
    val out    = new ByteArrayOutputStream()
    val stdOut = System.out
    System.setOut(new PrintStream(out))
    val result = body.unsafeRunSync()
    IO.sleep(200 millis).unsafeRunSync()
    System.setOut(stdOut)
    assertion(out.toString, result)
  }

  def testStdOut(
      body: IO[Unit],
      assertion: String => Assertion
  ) = testStdOut[Unit](body, (stdOut, _) => assertion(stdOut))
}
