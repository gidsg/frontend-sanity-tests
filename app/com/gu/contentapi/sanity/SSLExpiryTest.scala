package com.gu.contentapi.sanity

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import scala.util.{Try, Success, Failure}
import org.scalatest.{FlatSpec}
import javax.net.ssl._
import java.net.{URL}
import org.joda.time.{Days, DateTime}
import sun.security.x509.X509CertImpl

import scala.util.Try

class SSLExpiryTest extends FlatSpec with ScalaFutures with IntegrationPatience {

  "SSL Certificates" should "be more than 30 days from expiry" taggedAs (LowPriorityTest) in {
    handleException {
      val hosts = Seq(
        Config.host,
        Config.hostPublicSecure,
        Config.previewHost,
        Config.writeHost,
        Config.writePreviewHost
      )

      val secureHosts = hosts map {
        _.replaceAll("http://", "https://")
      }

      for (host <- secureHosts) {

        val url = new URL(host)
        val conn = url.openConnection().asInstanceOf[HttpsURLConnection]
        conn.setHostnameVerifier(new HostnameVerifier {
          override def verify(hostnameVerifier: String, sslSession: SSLSession) = true
        }
        )

        val certsTry = Try {
          conn.connect()
          conn.getServerCertificates
        }
        conn.disconnect()

        certsTry match {
          case Success(certs) =>
            withClue(s"No Certificates found for $host") {
              certs.length should be >= 1
            }
            certs.headOption.map { cert =>
              cert shouldBe a[X509CertImpl]
              val x = cert.asInstanceOf[X509CertImpl]
              val expiry = x.getNotAfter.getTime
              val daysleft = Days.daysBetween(new DateTime(), new DateTime(expiry.toLong)).getDays
              if (daysleft < 30) {
                fail("Cert for %s expires in %d days".format(host, daysleft))
              }
            }
          case Failure(e) =>
            cancel(s"Cancelling test as exception thrown: ${e.getClass.getSimpleName}")

        }

      }
    } (fail, testNames.head, tags)
  }
}