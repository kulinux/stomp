package com.stomp.ws

import akka.actor.{ Actor, Props }

object WsActor {
  def props = Props[WsActor]
}

class WsActor extends Actor {
  override def receive: Receive = {
    case msg => println(s"WsActor has receive a msg $msg")
  }
}
