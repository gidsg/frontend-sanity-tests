package com.gu.contentapi.sanity.support

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.{ServiceAbbreviations, Regions, Region}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.cloudwatch.model.{MetricDatum, PutMetricDataRequest}
import org.scalatest.{Succeeded, Failed, Outcome, Suite}
import play.api.{Logger, Configuration}

trait CloudWatchReportingSupport extends Suite {

  def cloudWatchReporter: CloudWatchReporter

  override protected def withFixture(test: NoArgTest): Outcome = {
    val outcome = super.withFixture(test)
    outcome match {
      case Failed(e) => cloudWatchReporter.reportFailedTest()
      case Succeeded => cloudWatchReporter.reportSuccessfulTest()
      case _ => // do nothing
    }
    outcome
  }

}

trait CloudWatchReporter {

  def reportSuccessfulTest(): Unit
  def reportFailedTest(): Unit
  def reportTestRunComplete(): Unit

}

object CloudWatchReporter {

  def apply(config: Configuration): CloudWatchReporter = {
    def cfg(key: String) = config.getString(s"content-api-sanity-tests.cloudwatch-$key")
    val reporter = for {
      namespace <- cfg("namespace")
      testRunsMetric <- cfg("test-runs-metric")
      successfulTestsMetric <- cfg("successful-tests-metric")
      failedTestsMetric <- cfg("failed-tests-metric")
    } yield {
      Logger.info(s"Will report metrics to CloudWatch. namespace=$namespace, metrics=($testRunsMetric, $successfulTestsMetric, $failedTestsMetric)")
      new RealCloudWatchReporter(namespace, testRunsMetric, successfulTestsMetric, failedTestsMetric)
    }

    reporter getOrElse {
      Logger.info("Will not report any metrics to CloudWatch")
      DoNothingCloudWatchReporter
    }
  }

}

class RealCloudWatchReporter(namespace: String,
                             testRunsMetric: String,
                             successfulTestsMetric: String,
                             failedTestsMetric: String) extends CloudWatchReporter {

  private val region = Region.getRegion(Regions.EU_WEST_1)

  lazy val cloudwatch = {
    val client = new AmazonCloudWatchAsyncClient()
    client.setEndpoint(region.getServiceEndpoint(ServiceAbbreviations.CloudWatch))
    client
  }

  object LoggingAsyncHandler extends AsyncHandler[PutMetricDataRequest, Void] {
    def onError(exception: Exception) {
      Logger.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
    }
    def onSuccess(request: PutMetricDataRequest, result: Void) {
      Logger.trace("CloudWatch PutMetricDataRequest - success")
    }
  }

  private def put(metricName: String, value: Double): Unit = {
    val metric = new MetricDatum()
      .withValue(value)
      .withMetricName(metricName)

    val request = new PutMetricDataRequest().withNamespace(namespace).withMetricData(metric)

    cloudwatch.putMetricDataAsync(request, LoggingAsyncHandler)
  }

  override def reportSuccessfulTest(): Unit = put(successfulTestsMetric, 1)

  override def reportFailedTest(): Unit = put(failedTestsMetric, 1)

  override def reportTestRunComplete(): Unit = put(testRunsMetric, 1)

}

object DoNothingCloudWatchReporter extends CloudWatchReporter {
  override def reportSuccessfulTest(): Unit = {}
  override def reportFailedTest(): Unit = {}
  override def reportTestRunComplete(): Unit = {}
}