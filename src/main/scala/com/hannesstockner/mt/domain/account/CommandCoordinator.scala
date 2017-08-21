package com.hannesstockner.mt.domain.account

import akka.actor.{Actor, Props}
import com.hannesstockner.mt.AccountId

class CommandCoordinator(toAccount: (AccountId) => Props) extends Actor {

  override def receive: Receive = {
    case c: OpenAccount =>
      val maybeChild = context.child(c.id.value)
      maybeChild match {
        case Some(_) =>
          sender() ! Left("Account already available")

        case None =>
          val child = context.actorOf(toAccount(c.id), c.id.value)
          child forward c
      }
    case c: AccountCommand =>
      val maybeChild = context.child(c.id.value)
      maybeChild match {
        case Some(child) =>
          child forward c

        case None =>
          sender() ! Left(s"Account with id ${c.id} not available")
      }
  }
}

object CommandCoordinator {

  def props(toAccount: (AccountId) => Props) =
    Props(new CommandCoordinator(toAccount = toAccount))
}


