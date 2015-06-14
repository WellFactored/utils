package com.wiredthing.utils.playSupport.filters

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}

class RewritePlay1AuthenticityTokenTest extends PlaySpec with OneAppPerSuite {
  implicit override lazy val app =
    FakeApplication(
      additionalConfiguration = Map("ehcacheplugin" -> "disabled", "play.crypto.secret" -> "secret")
    )

  "RewritePlay1AuthenticityToken.addSessionProperty" must {
    "add the property to an empty session" in {

      val sut = new RewritePlay1AuthenticityToken()
      sut.addSessionProperty(FakeRequest(POST, "/"), ("foo" -> "bar")).session.get("foo") mustBe Some("bar")

      "override the session if it exists" in {
        val sut = new RewritePlay1AuthenticityToken()
        val request = FakeRequest(POST, "/").withSession("foo" -> "baz")

        sut.addSessionProperty(request, ("foo" -> "bar")).session.get("foo") mustBe Some("bar")
      }

      "not touch any other session properties" in {
        val sut = new RewritePlay1AuthenticityToken()

        val request = FakeRequest(POST, "/").withSession("fib" -> "baz")

        sut.addSessionProperty(request, ("foo" -> "bar")).session.get("fib") mustBe Some("baz")
      }
    }
  }
}