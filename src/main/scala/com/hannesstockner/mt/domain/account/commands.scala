package com.hannesstockner.mt.domain.account

import com.hannesstockner.mt.{AccountId, Amount, DepositId, TransferId}

sealed trait AccountCommand {
  def id: AccountId
}

case class OpenAccount(id: AccountId = AccountId()) extends AccountCommand

case class DepositAccount(id: AccountId = AccountId(), depositId: DepositId, amount: Amount) extends AccountCommand

case class SendTransfer(id: AccountId, transferId: TransferId, to: AccountId, amount: Amount) extends AccountCommand

case class ReceiveTransfer(id: AccountId, transferId: TransferId, from: AccountId, amount: Amount) extends AccountCommand

case class CompensateFailedTransfer(id: AccountId, transferId: TransferId, to: AccountId, amount: Amount, reason: String) extends AccountCommand
