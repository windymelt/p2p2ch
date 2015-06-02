package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import java.net.URLEncoder

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends Specification {

  "Application" should {

    "return a bad request if it would be sent invalid request" in {
      running(FakeApplication()) {
        route(FakeRequest(GET, "/boum")) must beNone
      }
    }

    "contains text \"P2P2ch is (probably) ready.\" if it render the index page" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/")).get

        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
        contentAsString(home) must contain("P2P2ch is (probably) ready.")
      }
    }

    "contains the index page" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/index.html")).get

        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
      }
    }

/**
    "POST /test/bbs.cgi will succeed to post with ascii strings" in {
      running(FakeApplication()) {

        val data: Map[String, String] = Map(
          "bbs"     -> "test",
          "time"    -> String.valueOf(System.currentTimeMillis / 1000),
          "submit"  -> "",
          "FROM"    -> "nanashi",
          "mail"    -> "nanashi@example.com",
          "MESSAGE" -> "I am a Chaika",
          "subject" -> "This is it"
        )

        val bbs = route(FakeRequest(POST, "/test/bbs.cgi").withFormUrlEncodedBody(data.toSeq: _*)).get

        status(bbs) must equalTo(OK)
      }
    }
*/

    "GET /bbs/subject.txt can return subject.txt" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/bbs/subject.txt")).get

        println(contentAsString(home))
        status(home) must equalTo(OK)
      }
    }

    "GET /index.html can return threadlist with WEBUI" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/index.html")).get

        status(home) must equalTo(OK)
      }
    }
  }
}
