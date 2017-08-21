package com.hannesstockner.mt.domain.account

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.hannesstockner.mt._

class AccountProcessor(id: AccountId, publish: AccountEvent => Unit) extends PersistentActor with ActorLogging {
  override def persistenceId: String = id.value

  var account: Account = _

  override def receiveRecover = {
    case e: AccountEvent =>
      updateState(e)
  }

  def persistEvent(event: AccountEvent, sender: ActorRef) {
    persist(event) { e =>
      log.info(s"Event created ${e}")
      updateState(e)
      sender ! Right(e)
      publish(e)
    }
  }

  // logic

  override def receiveCommand = {
    case c: OpenAccount =>
      val event = AccountOpened(id = c.id, createdAt = CreatedAt())
      persistEvent(event, sender())

    case c: DepositAccount =>
      val newBalance = account.balance.deposit(c.amount)
      persistEvent(AccountDeposited(id = c.id, createdAt = CreatedAt(), depositId = c.depositId, amount = c.amount, balance = newBalance.value), sender())

    case c: SendTransfer =>
      val enoughBalance = !account.balance.withdrawWillCauseNegativeBalance(c.amount)
      if (enoughBalance) {
        val newBalance = account.balance.withdraw(c.amount)
        persistEvent(TransferSent(id = c.id, createdAt = CreatedAt(), transferId = c.transferId, to = c.to, amount = c.amount, balance = newBalance.value), sender())
      } else {
        sender ! Left("Not enough money on the account.")
      }

    case c: ReceiveTransfer =>
      val newBalance = account.balance.deposit(c.amount)
      persistEvent(TransferReceived(id = c.id, createdAt = CreatedAt(), transferId = c.transferId, from = c.from, amount = c.amount, balance = newBalance.value), sender())

    case c: CompensateFailedTransfer =>
      val newBalance = account.balance.deposit(c.amount)
      persistEvent(FailedTransferCompensated(id = c.id, createdAt = CreatedAt(), transferId = c.transferId, to = c.to, amount = c.amount, balance = newBalance.value, reason = c.reason), sender())
  }

  def updateState(event: AccountEvent) = event match {
    case AccountOpened(_, _) =>
      account = Account(balance = Balance(Amount(0)))

    case e: AccountDeposited =>
      account = account.copy(balance = Balance(e.balance), ledger = Credit(e.amount, e.depositId) :: account.ledger)

    case e: TransferSent =>
      account = account.copy(balance = Balance(e.balance), ledger = TransferDebit(e.amount, e.to, e.transferId) :: account.ledger)

    case e: TransferReceived =>
      account = account.copy(balance = Balance(e.balance), ledger = TransferCredit(e.amount, e.from, e.transferId) :: account.ledger)

    case e: FailedTransferCompensated =>
      account = account.copy(balance = Balance(e.balance), ledger = TransferDebitFailed(e.amount, e.to, e.transferId) :: account.ledger)
  }
}

object AccountProcessor {

  def props(publish: AccountEvent => Unit)(id: AccountId) =
    Props(new AccountProcessor(id = id, publish = publish))
}

case class Account(balance: Balance, ledger: List[Ledger] = List())

case class Balance(value: Amount) {

  def withdraw(amount: Amount): Balance = Balance(value = value.subtract(amount))

  def deposit(amount: Amount): Balance = Balance(value = value.add(amount))

  def withdrawWillCauseNegativeBalance(amount: Amount): Boolean = value.subtract(amount).isNegative()
}

sealed trait Ledger {
  def amount: Amount
}

case class Credit(amount: Amount, depositId: DepositId) extends Ledger
case class TransferCredit(amount: Amount, from: AccountId, transferId: TransferId) extends Ledger
case class TransferDebit(amount: Amount, to: AccountId, transferId: TransferId) extends Ledger
case class TransferDebitFailed(amount: Amount, to: AccountId, transferId: TransferId) extends Ledger
