package com.hannesstockner.mt.api

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.hannesstockner.mt.Context
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class AccountServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with ScalaFutures {

  "Accounts API" should {
    "Posting to /accounts should add an account" in {

      val postRequest = HttpRequest(HttpMethods.POST, uri = "/accounts")

      val routes = Context.start(system = system)

      postRequest ~> routes ~> check {
        status shouldEqual StatusCodes.Accepted
      }
    }
  }

}
