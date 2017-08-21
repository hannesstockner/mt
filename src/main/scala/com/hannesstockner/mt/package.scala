package com.hannesstockner

import java.util.UUID

import spray.json.{DefaultJsonProtocol, JsNumber, JsString, JsValue, RootJsonFormat}

package object mt {

  case class AccountId(value: String = UUID.randomUUID().toString) extends AnyVal

  case class TransferId(value: String = UUID.randomUUID().toString) extends AnyVal

  case class DepositId(value: String = UUID.randomUUID().toString) extends AnyVal

  case class Amount(value: Long) extends AnyVal {
    def subtract(amount: Amount): Amount = Amount(value = value - amount.value)

    def add(amount: Amount): Amount = Amount(value = value + amount.value)

    def isNegative(): Boolean = value < 0
  }

  case class CreatedAt(value: Long = System.currentTimeMillis()) extends AnyVal

  trait Event

  object JsonProtocol extends DefaultJsonProtocol {

    implicit object AccountIdJsonFormat extends RootJsonFormat[AccountId] {
      def write(o: AccountId) = JsString(o.value)
      def read(value: JsValue) = AccountId(value.convertTo[String])
    }

    implicit object TransferIdJsonFormat extends RootJsonFormat[TransferId] {
      def write(o: TransferId) = JsString(o.value)
      def read(value: JsValue) = TransferId(value.convertTo[String])
    }

    implicit object DepositIdJsonFormat extends RootJsonFormat[DepositId] {
      def write(o: DepositId) = JsString(o.value)
      def read(value: JsValue) = DepositId(value.convertTo[String])
    }

    implicit object MoneyJsonFormat extends RootJsonFormat[Amount] {
      def write(o: Amount) = JsNumber(o.value)
      def read(value: JsValue) = Amount(value.convertTo[Long])
    }
  }

}


