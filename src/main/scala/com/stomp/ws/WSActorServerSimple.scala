package com.stomp.ws

import akka.actor.ActorRef
import akka.cluster.client.ClusterClient.Publish
import akka.cluster.pubsub.DistributedPubSub
import com.stomp.ws.parser.StompMessage

class WSActorServerSimple(outActor: ActorRef) extends WSActorServer(outActor) {
  override def send(stm: StompMessage): Unit = {
    for{ dst <- stm.header.get("destination") } {
      val mediator = DistributedPubSub(context.system).mediator
      mediator ! Publish(dst, self)
    }
  }

}
