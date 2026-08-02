package io.dlinov.auth.routes.proxied

import org.http4s.Headers
import org.http4s.Header
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.typelevel.ci.CIString

class ProxySupportSpec extends AnyWordSpecLike with Matchers {

  private def raw(name: String, value: String): Header.Raw = Header.Raw(CIString(name), value)

  private def names(headers: Headers): Set[CIString] = headers.headers.map(_.name).toSet

  "ProxySupport.sanitizeRequestHeaders" should {
    "strip Authorization, Host and Connection" in {
      val in = Headers(
        raw("Authorization", "Bearer secret"),
        raw("Host", "client-supplied-host"),
        raw("Connection", "keep-alive"),
        raw("X-Keep", "should-remain")
      )
      val out = ProxySupport.sanitizeRequestHeaders(in)
      names(out) mustBe Set(CIString("X-Keep"))
    }

    "strip the fixed hop-by-hop header set" in {
      val in = Headers(
        raw("Keep-Alive", "timeout=5"),
        raw("Proxy-Authenticate", "Basic"),
        raw("Proxy-Authorization", "Basic secret"),
        raw("TE", "trailers"),
        raw("Trailer", "X-Checksum"),
        raw("Transfer-Encoding", "chunked"),
        raw("Upgrade", "websocket"),
        raw("X-Keep", "should-remain")
      )
      val out = ProxySupport.sanitizeRequestHeaders(in)
      names(out) mustBe Set(CIString("X-Keep"))
    }

    "strip extra headers named by a Connection header's value" in {
      val in = Headers(
        raw("Connection", "X-Custom-Hop"),
        raw("X-Custom-Hop", "should-be-stripped"),
        raw("X-Keep", "should-remain")
      )
      val out = ProxySupport.sanitizeRequestHeaders(in)
      names(out) mustBe Set(CIString("X-Keep"))
    }

    "leave ordinary end-to-end headers untouched" in {
      val in = Headers(
        raw("Content-Type", "application/json"),
        raw("Accept", "application/json"),
        raw("X-Request-Id", "abc-123")
      )
      val out = ProxySupport.sanitizeRequestHeaders(in)
      names(out) mustBe names(in)
    }
  }

  "ProxySupport.sanitizeResponseHeaders" should {
    "strip the same hop-by-hop header set on the way back" in {
      val in = Headers(
        raw("Connection", "keep-alive"),
        raw("Keep-Alive", "timeout=5"),
        raw("Transfer-Encoding", "chunked"),
        raw("Content-Type", "application/json")
      )
      val out = ProxySupport.sanitizeResponseHeaders(in)
      names(out) mustBe Set(CIString("Content-Type"))
    }
  }
}
