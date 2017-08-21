package com.hannesstockner.mt.functional

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import com.hannesstockner.mt.{Amount, Context}
import com.hannesstockner.mt.api.JsonSupport
import com.hannesstockner.mt.report.AccountReport
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class FunctionalSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with ScalaFutures with JsonSupport {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val port = 8006
  val host = s"http://localhost:${port}"

  val openAccountRequest = HttpRequest(HttpMethods.POST, uri = s"${host}/accounts")

  override def beforeAll(): Unit = Context.start(system, port)

  "Accounts API" should {
    "Posting to /accounts should add an account" in {

      val response = Http().singleRequest(openAccountRequest).futureValue

      response.status shouldBe StatusCodes.Accepted
      val headersMap = response.headers.map(i => i.name() -> i.value()).toMap
      assert(headersMap("Location").startsWith(host))
    }

    "Get to /accounts/{id} should show an account" in {
      val response = Http().singleRequest(openAccountRequest).futureValue
      val headersMap = response.headers.map(i => i.name() -> i.value()).toMap
      val accountLocation = headersMap("Location")
      val responseGet = Http().singleRequest(HttpRequest(HttpMethods.GET, uri = accountLocation)).futureValue

      responseGet.status shouldBe StatusCodes.OK
      val report = Unmarshal(responseGet.entity).to[AccountReport].futureValue

      report.balance shouldBe Amount(0)
      report.transfers shouldBe List()
      report.deposits shouldBe List()
    }

    "Post to /accounts/{id}/deposits should deposit" in {
      val response = Http().singleRequest(openAccountRequest).futureValue
      val headersMap = response.headers.map(i => i.name() -> i.value()).toMap
      val accountLocation = headersMap("Location")

      val jsonRequest = ByteString(
        s"""
           |{
           |    "amount": 10000
           |}
        """.stripMargin)
      val responseDeposit = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"${accountLocation}/deposits", entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))).futureValue

      val responseGet = Http().singleRequest(HttpRequest(HttpMethods.GET, uri = accountLocation)).futureValue

      responseGet.status shouldBe StatusCodes.OK
      val report = Unmarshal(responseGet.entity).to[AccountReport].futureValue

      report.balance shouldBe Amount(10000)
      report.transfers.size shouldBe 0
      report.deposits.size shouldBe 1
    }

    "Post to /accounts/{id}/transfers should transfer" in {
      val responseFrom = Http().singleRequest(openAccountRequest).futureValue
      val headersMapFrom = responseFrom.headers.map(i => i.name() -> i.value()).toMap
      val accountLocationFrom = headersMapFrom("Location")

      val responseTo = Http().singleRequest(openAccountRequest).futureValue
      val headersMapTo = responseTo.headers.map(i => i.name() -> i.value()).toMap
      val accountLocationTo = headersMapTo("Location")

      val jsonRequestDeposit = ByteString(
        s"""
           |{
           |    "amount": 10000
           |}
        """.stripMargin)
      val responseDeposit = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"${accountLocationFrom}/deposits", entity = HttpEntity(MediaTypes.`application/json`, jsonRequestDeposit))).futureValue

      val to = accountLocationTo.substring(accountLocationTo.lastIndexOf("/") + 1)

      val jsonRequestTransfer = ByteString(
        s"""
           |{
           |    "to": "${to}",
           |    "amount": 500
           |}
        """.stripMargin)
      val responseTransfer = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"${accountLocationFrom}/transfers", entity = HttpEntity(MediaTypes.`application/json`, jsonRequestTransfer))).futureValue

      val responseGetFrom = Http().singleRequest(HttpRequest(HttpMethods.GET, uri = accountLocationFrom)).futureValue

      responseGetFrom.status shouldBe StatusCodes.OK
      val reportFrom = Unmarshal(responseGetFrom.entity).to[AccountReport].futureValue

      reportFrom.balance shouldBe Amount(9500)
      reportFrom.transfers.size shouldBe 1
      reportFrom.deposits.size shouldBe 1

      val responseGetTo = Http().singleRequest(HttpRequest(HttpMethods.GET, uri = accountLocationTo)).futureValue

      responseGetTo.status shouldBe StatusCodes.OK
      val reportTo = Unmarshal(responseGetTo.entity).to[AccountReport].futureValue

      reportTo.balance shouldBe Amount(500)
      reportTo.transfers.size shouldBe 1
      reportTo.deposits.size shouldBe 0
    }

    "Post to /accounts/{id}/transfers should compensate because to account not available" in {
      val responseFrom = Http().singleRequest(openAccountRequest).futureValue
      val headersMapFrom = responseFrom.headers.map(i => i.name() -> i.value()).toMap
      val accountLocationFrom = headersMapFrom("Location")

      val responseTo = Http().singleRequest(openAccountRequest).futureValue
      val headersMapTo = responseTo.headers.map(i => i.name() -> i.value()).toMap
      val accountLocationTo = headersMapTo("Location")

      val jsonRequestDeposit = ByteString(
        s"""
           |{
           |    "amount": 10000
           |}
        """.stripMargin)
      val responseDeposit = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"${accountLocationFrom}/deposits", entity = HttpEntity(MediaTypes.`application/json`, jsonRequestDeposit))).futureValue

      val to = accountLocationTo.substring(accountLocationTo.lastIndexOf("/") + 1)

      val jsonRequestTransfer = ByteString(
        s"""
           |{
           |    "to": "abc",
           |    "amount": 500
           |}
        """.stripMargin)
      val responseTransfer = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"${accountLocationFrom}/transfers", entity = HttpEntity(MediaTypes.`application/json`, jsonRequestTransfer))).futureValue

      val responseGet = Http().singleRequest(HttpRequest(HttpMethods.GET, uri = accountLocationFrom)).futureValue

      responseGet.status shouldBe StatusCodes.OK
      val report = Unmarshal(responseGet.entity).to[AccountReport].futureValue

      report.balance shouldBe Amount(10000)
      report.transfers.size shouldBe 2
      report.deposits.size shouldBe 1
    }

    "Post to /accounts/{id}/transfers should not transfer because not enough money" in {
      val responseFrom = Http().singleRequest(openAccountRequest).futureValue
      val headersMapFrom = responseFrom.headers.map(i => i.name() -> i.value()).toMap
      val accountLocationFrom = headersMapFrom("Location")

      val responseTo = Http().singleRequest(openAccountRequest).futureValue
      val headersMapTo = responseTo.headers.map(i => i.name() -> i.value()).toMap
      val accountLocationTo = headersMapTo("Location")

      val jsonRequestDeposit = ByteString(
        s"""
           |{
           |    "amount": 10000
           |}
        """.stripMargin)
      val responseDeposit = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"${accountLocationFrom}/deposits", entity = HttpEntity(MediaTypes.`application/json`, jsonRequestDeposit))).futureValue

      val to = accountLocationTo.substring(accountLocationTo.lastIndexOf("/") + 1)

      val jsonRequestTransfer = ByteString(
        s"""
           |{
           |    "to": "${to}",
           |    "amount": 500000
           |}
        """.stripMargin)
      val responseTransfer = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = s"${accountLocationFrom}/transfers", entity = HttpEntity(MediaTypes.`application/json`, jsonRequestTransfer))).futureValue

      val responseGet = Http().singleRequest(HttpRequest(HttpMethods.GET, uri = accountLocationFrom)).futureValue

      responseGet.status shouldBe StatusCodes.OK
      val report = Unmarshal(responseGet.entity).to[AccountReport].futureValue

      report.balance shouldBe Amount(10000)
      report.transfers.size shouldBe 0
      report.deposits.size shouldBe 1
    }
  }

}
