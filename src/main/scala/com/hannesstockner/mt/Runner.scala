package com.hannesstockner.mt

import akka.actor.ActorSystem

object Runner extends App {
  val system = ActorSystem("mt")

  Context.start(system)
}



