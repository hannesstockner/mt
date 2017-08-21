package com.hannesstockner.mt.report

import java.time.{Instant, ZoneOffset, ZonedDateTime}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.hannesstockner.mt.{AccountId, Amount, CreatedAt, Event}
import com.hannesstockner.mt.domain.account.{AccountDeposited, AccountEvent, AccountOpened, FailedTransferCompensated, TransferReceived, TransferSent}
import com.hannesstockner.mt.report.AccountReportRepo.toIsoDateString

class AccountReportRepo(subscribe: (ActorRef, Class[_]) => Boolean) extends Actor with ActorLogging {

  var reportByAccountId = Map.empty[AccountId, AccountReport]

  override def preStart = {
    subscribe(self, classOf[AccountOpened])
    subscribe(self, classOf[AccountDeposited])
    subscribe(self, classOf[TransferSent])
    subscribe(self, classOf[TransferReceived])
    subscribe(self, classOf[FailedTransferCompensated])
  }

  override def receive = {
    case e: AccountEvent =>
      reportByAccountId = reportByAccountId + (e.id -> applyEventToAccountReport(e, reportByAccountId.get(e.id)))

    case q: AccountReportByAccountId =>
      val maybeAccountReport = reportByAccountId.get(q.id)
      sender() ! maybeAccountReport
  }

  def applyEventToAccountReport(event: Event, maybeAccountReport: Option[AccountReport]): AccountReport = {
    maybeAccountReport match {
      case None => event match {
        case e: AccountOpened => AccountReport(id = e.id, balance = Amount(0), transfers = Seq.empty, deposits = Seq.empty)
      }
      case Some(state) => {
        val newState = event match {
          case e: AccountDeposited =>
            state.copy(balance = e.balance, deposits = DepositReport(id = e.depositId, createdAt = toIsoDateString(e.createdAt), amount = e.amount, balance = e.balance) :: state.deposits.toList)
          case e: TransferSent =>
            state.copy(balance = e.balance, transfers = TransferReport(id = e.transferId, createdAt = toIsoDateString(e.createdAt), from = e.id, to = e.to, amount = e.amount, balance = e.balance, status = TransferSentStatus) :: state.transfers.toList)
          case e: TransferReceived =>
            state.copy(balance = e.balance, transfers = TransferReport(id = e.transferId, createdAt = toIsoDateString(e.createdAt), from = e.from, to = e.id, amount = e.amount, balance = e.balance, status = TransferReceivedStatus) :: state.transfers.toList)
          case e: FailedTransferCompensated =>
            state.copy(balance = e.balance, transfers = TransferReport(id = e.transferId, createdAt = toIsoDateString(e.createdAt), from = e.id, to = e.to, amount = e.amount, balance = e.balance, status = TransferFailedStatus, comment = Some(e.reason)) :: state.transfers.toList)
        }
        newState
      }
    }
  }
}

object AccountReportRepo {

  def props(subscribe: (ActorRef, Class[_]) => Boolean) =
    Props(new AccountReportRepo(subscribe = subscribe))

  def toIsoDateString(ms: CreatedAt): String = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms.value), ZoneOffset.UTC).toString
}
