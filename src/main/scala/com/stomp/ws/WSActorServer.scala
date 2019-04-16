package com.stomp.ws

import java.util.UUID

import akka.actor.{Actor, ActorRef, Cancellable}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe, UnsubscribeAck}
import com.stomp.ws.parser.StompMessage

import scala.collection.mutable
import scala.concurrent.duration._



class WSActorServer(outActor: ActorRef) extends Actor {

  val subscriptions: mutable.Map[String, String] = mutable.Map()
  var pongScheduler = Option.empty[Cancellable]


  override def receive: Receive = {
    case WSActorServer.Init => {
      sender() ! WSActorServer.Ack
      context.become(idle)
    }
    case other => println(s"Other received $other")
  }


  override def postStop(): Unit = {
    super.postStop()

    for{ ch <- pongScheduler } ch.cancel()
  }

  def idle: Receive = {
    case sm @ StompMessage(StompMessage.Connect, _, _) => {
      connect(sm)
      context.become(connected)
    }
  }



  def connected: Receive = {
    case sm @ StompMessage(StompMessage.Send, _, _) => send(sm)
    case sm @ StompMessage(StompMessage.Subscribe, _, _) => subscribe(sm)
    case sm @ StompMessage(StompMessage.UnSubscribe, _, _) => unsubscribe(sm)
    case sm @ StompMessage("", _, _) => ping(sm)
    case WSActorServer.Complete => println("Complete")
    case SubscribeAck(Subscribe(channel, _, _)) => subscribed(channel)
    case UnsubscribeAck(Unsubscribe(channel, _, _)) => unsubscribed(channel)
    case "PONG" => pong()
    case um => println(s"Unknown Message $um")
  }

  //Messages Handler//

  def subscribed(channel: String): Unit = { println("subscribed ack") }

  def unsubscribed(channel: String): Unit = { println("unsubscribed ack")}

  def subscribe(sm: StompMessage): Unit = {
    val mediator = DistributedPubSub(context.system).mediator

    for {
      id <- sm.header.get("id")
      chn <- sm.header.get("destination")
    } {
      mediator ! Subscribe(chn, self)
      subscriptions.put(id, chn)
    }

    sender() ! WSActorServer.Ack
  }

  def ping(sm: StompMessage): Unit = {
    sender() ! WSActorServer.Ack
  }

  def pong(): Unit = {
    outActor ! StompMessage( "" )
  }


  def startPong(timeout: Int) = {
    implicit val dispatcher = context.dispatcher
    pongScheduler = Some(
      context.system.scheduler.schedule(
        timeout milliseconds,
        timeout milliseconds,
        self,
        "PONG")
    )
  }

  def unsubscribe(sm: StompMessage): Unit = {
    val mediator = DistributedPubSub(context.system).mediator
    for {
      id <- sm.header.get("id")
      chn <- subscriptions.get(id)
    } {
      mediator ! Unsubscribe(chn, self)
      subscriptions.remove(id)
    }

    sender() ! WSActorServer.Ack
  }


  def connect(stm: StompMessage) = {
    val heartBeat =
      stm.header.get("heart-beat")
        .filter(_.contains(","))
        .map( _.split(","))
        .map(hb => (hb(0).toInt, hb(1).toInt))
        .getOrElse((0, 0))
    if(heartBeat._2 > 0) {
      startPong(heartBeat._2)
    }

    outActor ! StompMessage(
      StompMessage.Connected,
      Map(
        "version" -> "1.2",
        "heart-beat" -> Seq(heartBeat._2, heartBeat._1).mkString(","),
        "server" -> "StompScala/1.0"))

    sender() ! WSActorServer.Ack
  }

  def send(stm: StompMessage) = {
    println(s"SEND $stm")
    sender() ! WSActorServer.Ack
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
