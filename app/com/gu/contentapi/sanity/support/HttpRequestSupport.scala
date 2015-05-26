package com.gu.contentapi.sanity.support

import com.gu.contentapi.sanity.Config
import org.scalatest.{Assertions, Matchers}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS, WSRequestHolder}
import play.api.Play.current

trait HttpRequestSupport extends ScalaFutures with Matchers with Assertions {

  def request(uri: String): WSRequestHolder = WS.url(uri).withRequestTimeout(10000)

  def requestHost(path: String) =
    // make sure query string is included
    if (path.contains("?"))
      request(Config.host + path + "&api-key=" + Config.apiKey)
    else
      request(Config.host + path + "?api-key=" + Config.apiKey)

  def isCAPIShowingChange(capiURI: String, modifiedString: String, credentials: Option[(String, String)] = None) = {
    val httpRequest = credentials match {
      case Some((username, password)) =>
        request(capiURI).withAuth(Config.previewUsernameCode, Config.previewPasswordCode, WSAuthScheme.BASIC)
      case None =>
        request(capiURI)
    }
    whenReady(httpRequest.get()) { result =>
      withClue("Authentication failed, check credentials") {
        result.status should not equal 401
      }
      result.body.contains(modifiedString)
    }
  }

  def assumeNot5xxResponse(response: WSResponse): Unit = {
    assume(response.status !=503,"Service is down")
    assume(response.status !=504,"ELB timeout")
  }

}
