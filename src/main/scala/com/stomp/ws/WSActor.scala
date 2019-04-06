package com.stomp.ws

import akka.actor.{Actor, ActorRef}
import com.stomp.ws.parser.StompMessage

class WSActor(outActor: ActorRef) extends Actor {

  override def receive: Receive = {
    case WSActor.Init => {
      println("Init received")
      sender() ! WSActor.Ack
    }
    case sm: StompMessage => {
      println(s"StompMessage $sm")
      outActor ! new StompMessage("MONOSM")
      sender() ! WSActor.Ack
    }
    case WSActor.Complete => println("Complete")
    case other => println(s"Other received $other")
  }
}

object WSActor {
  case object Init
  case object Ack
  case object Complete
}
