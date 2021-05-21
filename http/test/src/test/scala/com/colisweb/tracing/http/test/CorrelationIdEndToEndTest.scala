package com.colisweb.tracing.http.test

import cats.effect.{ExitCode, IO, Resource}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import io.circe.generic.auto._
import io.circe.parser._
import org.http4s.server.Server
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import cats.effect.Temporal

final class CorrelationIdEndToEndTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val cs: ContextShift[IO]         = IO.contextShift(ec)
  implicit val timer: Temporal[IO]             = IO.timer(ec)

  val (server1Port, server2Port) = (freePort, freePort)

  lazy val (wireMockServer, wireMock) = {
    val wireMockConfiguration =
      WireMockConfiguration.options().port(server2Port).extensions(new ResponseTemplateTransformer(true))
    (new WireMockServer(wireMockConfiguration), new WireMock("localhost", server2Port))
  }

  val server1: Resource[IO, Server[IO]] = TestServer.create[IO](serverPort = server1Port, server2Port = server2Port)

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    wireMock.register(
      get(urlPathEqualTo("/where_the_weed_at")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("""{ "correlationId": "{{request.headers.X-Correlation-Id}}" }""")
      )
    )
    server1.use(_ => IO.never).as(ExitCode.Success).unsafeRunAsyncAndForget()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  "emitting a request to the first server with a correlation id" should "give us the same correlation id in the response" in {
    val correlationId = "50c76f05-b422-47ac-86b5-5691a68f1ac9"
    val response =
      requests.get(
        url = s"http://localhost:$server1Port/pass_the_weed",
        headers = List(("x-correlation-id", correlationId))
      )

    decode[WrappedCorrelationId](response.text()) match {
      case Right(WrappedCorrelationId(c)) => c shouldBe correlationId
      case _                              => fail
    }
  }

  "emitting a request to the first server without a correlation id" should "generate a correlation id in the response" in {
    val response = requests.get(url = s"http://localhost:$server1Port/pass_the_weed")

    decode[WrappedCorrelationId](response.text()) match {
      case Right(_) => succeed
      case _        => fail
    }
  }

}
