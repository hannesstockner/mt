package com.hannesstockner.mt.report

import com.hannesstockner.mt.{AccountId, Amount, DepositId, TransferId}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

case class AccountReport(id: AccountId, balance: Amount, transfers: Seq[TransferReport], deposits: Seq[DepositReport])

case class TransferReport(id: TransferId, createdAt: String, from: AccountId, to: AccountId, amount: Amount, balance: Amount, status: TransferReportStatus, comment: Option[String] = None)

sealed trait TransferReportStatus {
  def name: String
}
case object TransferSentStatus extends TransferReportStatus {
  override val name: String = "TRANSFER_SENT"
}
case object TransferReceivedStatus extends TransferReportStatus {
  override val name: String = "TRANSFER_RECEIVED"
}
case object TransferFailedStatus extends TransferReportStatus {
  override val name: String = "TRANSFER_FAILED"
}

case class DepositReport(id: DepositId, createdAt: String, amount: Amount, balance: Amount)

object JsonProtocol extends DefaultJsonProtocol {

  import com.hannesstockner.mt.JsonProtocol._

  implicit object TransferReportStatusJsonFormat extends RootJsonFormat[TransferReportStatus] {
    def write(o: TransferReportStatus) = JsString(o.name)
    def read(value: JsValue) = value.convertTo[String] match {
      case TransferSentStatus.name => TransferSentStatus
      case TransferReceivedStatus.name => TransferReceivedStatus
      case TransferFailedStatus.name => TransferFailedStatus
    }
  }

  implicit val transferReportFormat = jsonFormat8(TransferReport)
  implicit val depositReportFormat = jsonFormat4(DepositReport)
  implicit val accountReportFormat = jsonFormat4(AccountReport)
}