package com.hannesstockner.mt.domain.account

import com.hannesstockner.mt._

sealed trait AccountEvent extends Event {
  def id: AccountId
  def createdAt: CreatedAt
}

case class AccountOpened(id: AccountId, createdAt: CreatedAt) extends AccountEvent

case class AccountDeposited(id: AccountId, createdAt: CreatedAt, depositId: DepositId, amount: Amount, balance: Amount) extends AccountEvent

case class TransferSent(id: AccountId, createdAt: CreatedAt, transferId: TransferId, to: AccountId, amount: Amount, balance: Amount) extends AccountEvent

case class TransferReceived(id: AccountId, createdAt: CreatedAt, transferId: TransferId, from: AccountId, amount: Amount, balance: Amount) extends AccountEvent

case class FailedTransferCompensated(id: AccountId, createdAt: CreatedAt, transferId: TransferId, to: AccountId, amount: Amount, balance: Amount, reason: String) extends AccountEvent
