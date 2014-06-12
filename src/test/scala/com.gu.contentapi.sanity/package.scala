package com.gu.contentapi

import play.api.libs.ws.WS
import org.scalatest.exceptions.TestFailedException
import com.ning.http.client.Realm.AuthScheme
import org.scalatest.concurrent.ScalaFutures

package object sanity extends ScalaFutures {

  def request(uri: String) = WS.url(uri).withRequestTimeout(10000)

  def isCAPIShowingChange(capiURI: String, modifiedString: String, credentials: Option[(String,String)] = None) = {

    val httpRequest = credentials match {
      case Some((username, password)) =>
        request(capiURI).withAuth(Config.previewUsernameCode, Config.previewPasswordCode, AuthScheme.BASIC)
        case None => {
      request(capiURI)
        }
    }
    whenReady(httpRequest.get) { result => result.body.contains(modifiedString)}
  }

  def requestHost(path: String) =
  // make sure query string is included
    if(path.contains("?"))
    request(Config.host + path + "&api-key=" + Config.apiKey)
    else
    request(Config.host + path + "?api-key=" + Config.apiKey)
}
