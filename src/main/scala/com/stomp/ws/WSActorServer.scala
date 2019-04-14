package com.stomp.ws

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import com.stomp.ws.parser.StompMessage



class WSActorServer(outActor: ActorRef) extends Actor {


  override def preStart() = {
    println (s"PRESTART $self")
  }

  override def receive: Receive = {
    case WSActorServer.Init => {
      println("Init received")
      sender() ! WSActorServer.Ack
    }
    case sm: StompMessage => {
      println(s"StompMessage $sm")
      stompMessage(sm)
      sender() ! WSActorServer.Ack
      test()
    }
    case WSActorServer.Complete => println("Complete")
    case other => println(s"Other received $other")
  }

  def stompMessage(sm: StompMessage): Unit = {
    sm.command match {
      case StompMessage.Connect => connect(sm)
      case StompMessage.Send => send(sm)
      case _ => Some(StompMessage("ERROR", Map(), s"Unknow command ${sm.command}"))
    }
  }

  def connect(stm: StompMessage) = {

    val uuid = UUID.randomUUID().toString

    outActor ! StompMessage(
      StompMessage.Connected,
      Map(
        "version" -> "1.2",
        "heart-beat" -> "0,0",
        "session" -> uuid,
        "server" -> "StompScala/1.0"))
  }

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

object WSActorServer {
  case object Init
  case object Ack
  case object Complete
}
