package com.hannesstockner.mt.domain.transfer

import akka.actor.{Actor, ActorRef, Props}
import com.hannesstockner.mt.TransferId
import com.hannesstockner.mt.domain.account.{TransferReceived, TransferSent}

class EventCoordinator(toTransfer: (TransferId) => Props, subscribe: (ActorRef, Class[_]) => Boolean) extends Actor {

  override def preStart = {
    subscribe(self, classOf[TransferSent])
    subscribe(self, classOf[TransferReceived])
  }

  override def receive: Receive = {
    case e: TransferSent =>
      val maybeChild = context.child(e.transferId.value)
      maybeChild match {
        case Some(_) =>
          sender() ! Left("Message already processed")

        case None =>
          val child = context.actorOf(toTransfer(e.transferId), e.transferId.value)
          child forward e
      }
    case e: TransferReceived =>
      val maybeChild = context.child(e.transferId.value)
      maybeChild match {
        case Some(child) =>
          child forward e

        case None =>
          sender() ! Left(s"Transfer with id ${e.transferId} not available")
      }
  }
}

object EventCoordinator {

  def props(toTransfer: (TransferId) => Props, subscribe: (ActorRef, Class[_]) => Boolean) =
    Props(new EventCoordinator(toTransfer = toTransfer, subscribe = subscribe))
}


