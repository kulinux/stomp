package com.stomp.ws

import akka.actor.{Actor, Props}

class WSActorClient(session: String) extends Actor {
  override def receive: Receive = {
    case msg => println(s" WSActorClient: $msg")
  }
}

object WSActorClient {
  def props(session: String) = Props(new WSActorClient(session))
}


