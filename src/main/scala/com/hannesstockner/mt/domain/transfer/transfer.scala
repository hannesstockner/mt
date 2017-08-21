package com.hannesstockner.mt.domain.transfer

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import com.hannesstockner.mt.{AccountId, Amount, CreatedAt, TransferId}
import com.hannesstockner.mt.domain.account._

import scala.reflect.ClassTag

class TransferFSM(id: TransferId, commandCoordinator: ActorRef)
                 (implicit val domainEventClassTag: ClassTag[TransferStarted]) extends PersistentFSM[TransferState, TransferStateData, TransferStarted] {

  override def persistenceId: String = id.value

  startWith(stateName = Idle, stateData = EmptyTransfer)

  when(Idle) {
    case Event(TransferSent(accountId, _, transferId, to, amount, _), _) =>
      goto(Sent) applying TransferStarted(id = transferId, createdAt = CreatedAt(), from = accountId, to = to, amount = amount) andThen {
        case _ => commandCoordinator ! ReceiveTransfer(id = to, transferId = transferId, from = accountId, amount = amount)
      }
  }

  when(Sent) {
    case Event(Right(TransferReceived(_, _, _, _, _, _)), _) =>
      stop()

    case Event(Left(v), t) =>
      t match {
        case transfer: Transfer =>
          commandCoordinator ! CompensateFailedTransfer(id = transfer.from, transferId = id, to = transfer.to, amount = transfer.amount, reason = v.toString)
        case _ => Unit
      }
      stop()
  }

  override def applyEvent(domainEvent: TransferStarted, currentData: TransferStateData): TransferStateData = domainEvent match {
    case e: TransferStarted => Transfer(from = e.from, to = e.to, amount = e.amount)
  }
}

object TransferFSM {

  def props(commandCoordinator: ActorRef)(id: TransferId) =
    Props(new TransferFSM(id = id, commandCoordinator = commandCoordinator))
}

//stateData
sealed trait TransferStateData

case class Transfer(from: AccountId, to: AccountId, amount: Amount) extends TransferStateData
case object EmptyTransfer extends TransferStateData

//event
case class TransferStarted(id: TransferId, createdAt: CreatedAt, from: AccountId, to: AccountId, amount: Amount)

//states

sealed trait TransferState extends FSMState

case object Idle extends TransferState {
  override def identifier: String = "Idle"
}

case object Sent extends TransferState {
  override def identifier: String = "Sent"
}