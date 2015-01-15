package com.gu.contentapi.sanity

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, FlatSpec}
import play.api.libs.json.Json

class ValidateArticleSchema extends FlatSpec with Matchers with ScalaFutures with IntegrationPatience {

  "The Content API" should "return correct article schema" taggedAs(FrequentTest, PRODTest)  in {

    handleException {
      val httpRequest = requestHost("/lifeandstyle/lostinshowbiz/2014/sep/18/charlotte-crosby-blueprint-for-civilisation-geordie-shore-celebrity-big-brother?show-fields=all&show-elements=all").get
      whenReady(httpRequest) { result =>
        assume(result.status == 200, "Service is down")
        val json = Json.parse(result.body)
        val sectionName = (json \ "response" \ "content" \ "sectionName").as[String]
        sectionName should be ("Life and style")
        val headline = (json \ "response" \ "content" \ "fields" \ "headline").as[String]
        headline should be ("Charlotte Crosby: a blueprint for civilisation")
        val mediaId = ((json \ "response" \ "content" \ "elements")(0) \\ "mediaId")
        mediaId shouldBe a [List[_]]
      }
    }(fail,testNames.head, tags)
  }

}