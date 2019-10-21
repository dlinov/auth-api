package io.dlinov.auth.routes

import cats.effect.IO
import org.http4s._
import org.scalatest.{Assertion, MustMatchers}

trait Http4sSpecHelper { self: MustMatchers ⇒

  def skipBodyCheck(r: String): Assertion = succeed

  def check(actual: IO[Response[IO]], expectedStatus: Status): Assertion = {
    check[String](actual, expectedStatus, skipBodyCheck)
  }

  def check[T](actual: IO[Response[IO]], expectedStatus: Status, validateBody: T ⇒ Assertion)(
      implicit ev: EntityDecoder[IO, T]
  ): Assertion = {
    val actualResp = actual.unsafeRunSync
    actualResp.status mustBe expectedStatus
    // body bytes (might be needed in future): actualResp.body.compile.toVector.unsafeRunSync
    validateBody(actualResp.as[T].unsafeRunSync())
  }

  def checkRawBody(
      actual: IO[Response[IO]],
      expectedStatus: Status,
      validateBody: String ⇒ Assertion
  ): Assertion = {
    val actualResp = actual.unsafeRunSync
    actualResp.status mustBe expectedStatus
    val actualBodyAsString = new String(actualResp.body.compile.toVector.unsafeRunSync.toArray)
    validateBody(actualBodyAsString)
  }

  def checkEndpointExists(actual: IO[Response[IO]]): Assertion = {
    val actualResp = actual.unsafeRunSync
    val is404      = actualResp.status == Status.NotFound
    if (is404) {
      val actualBodyAsString = new String(actualResp.body.compile.toVector.unsafeRunSync.toArray)
      actualBodyAsString must not contain """"msg":"""""
    } else {
      succeed
    }
  }

  def buildGetRequest(uri: String): Request[IO] =
    Request[IO](method = Method.GET, uri = Uri.unsafeFromString(uri))

  def buildGetRequest(uri: String, token: String): Request[IO] =
    buildGetRequest(uri)
      .withHeaders(headers.Authorization(Credentials.Token(AuthScheme.Bearer, token)))

  def buildPostRequest[T](uri: String, entity: T)(
      implicit encoder: EntityEncoder[IO, T]
  ): Request[IO] =
    Request[IO](method = Method.POST, uri = Uri.unsafeFromString(uri))
      .withEntity(entity)

  def buildPostRequest[T](uri: String, entity: T, token: String)(
      implicit encoder: EntityEncoder[IO, T]
  ): Request[IO] =
    buildPostRequest(uri, entity)
      .withHeaders(
        headers.`Content-Type`(MediaType.application.json),
        headers.Authorization(Credentials.Token(AuthScheme.Bearer, token))
      )

  def buildPutRequest[T](uri: String, entity: T)(
      implicit encoder: EntityEncoder[IO, T]
  ): Request[IO] =
    Request[IO](method = Method.PUT, uri = Uri.unsafeFromString(uri))
      .withEntity(entity)

  def buildPutRequest[T](uri: String, entity: T, token: String)(
      implicit encoder: EntityEncoder[IO, T]
  ): Request[IO] =
    buildPutRequest(uri, entity)
      .withHeaders(
        headers.`Content-Type`(MediaType.application.json),
        headers.Authorization(Credentials.Token(AuthScheme.Bearer, token))
      )

  def buildDeleteRequest(uri: String): Request[IO] =
    Request[IO](method = Method.DELETE, uri = Uri.unsafeFromString(uri))

  def buildDeleteRequest(uri: String, token: String): Request[IO] =
    buildDeleteRequest(uri)
      .withHeaders(headers.Authorization(Credentials.Token(AuthScheme.Bearer, token)))

  def buildDeleteRequest[T](uri: String, entity: T, token: String)(
      implicit encoder: EntityEncoder[IO, T]
  ): Request[IO] =
    buildDeleteRequest(uri, token)
      .withHeaders(headers.`Content-Type`(MediaType.application.json))
      .withEntity(entity)
}
