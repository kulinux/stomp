package com.stomp.ws

import akka.actor.{ Actor, ActorRef }
import com.stomp.ws.parser.StompMessage

class WSActor(outActor: ActorRef) extends Actor {

  override def receive: Receive = {
    case WSActor.Init => {
      println("Init received")
      sender() ! WSActor.Ack
    }
    case sm: StompMessage => {
      println(s"StompMessage $sm")
      stompMessage(sm)
      sender() ! WSActor.Ack
      test()
    }
    case WSActor.Complete => println("Complete")
    case other => println(s"Other received $other")
  }

  def stompMessage(sm: StompMessage): Unit = {
    sm.command match {
      case StompMessage.Connect => Some(connect(sm))
      case StompMessage.Send => { send(sm); None }
      case _ => Some(StompMessage("ERROR", Map(), s"Unknow command ${sm.command}"))
    }
  }

  def connect(stm: StompMessage) =
    outActor ! StompMessage(
      StompMessage.Connected,
      Map(
        "version" -> "1.2",
        "heart-beat" -> "0,0",
        "server" -> "StompScala/1.0"))

  def send(stm: StompMessage) = {
    println(s"SEND $stm")
  }

  def test() = {
    outActor ! StompMessage(
      StompMessage.Message,
      Map(
        "subscription" -> "sub-0",
        "message-id" -> "sub-0",
        "destination" -> "/destination",
        "content-type" -> "text/plain"),
      "This is message")
  }

}

object WSActor {
  case object Init
  case object Ack
  case object Complete
}
