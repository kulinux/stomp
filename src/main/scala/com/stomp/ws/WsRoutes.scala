package com.stomp.ws

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.stomp.ws.parser.StompMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._


trait WsRoutes {

  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
  val ec = ExecutionContext.global

  def actorRef(outActor: ActorRef): ActorRef = system.actorOf(Props(new WSActorServer(outActor)))


  lazy val outSource : Source[TextMessage, ActorRef] =
    Source.actorRef[StompMessage](1, OverflowStrategy.fail)
    .map(st => StompMessage.marshallImpl(st))
    .map(str => TextMessage(str))


  lazy val toStrict: Flow[Message, String, _] = Flow[Message]
    .map(f => f.asTextMessage.asScala.toStrict(50 seconds).map(f => f.text)(system.dispatcher))
    .mapAsync(parallelism = 1)(identity)

  lazy val inFlow: Flow[Message, StompMessage, _] =
    toStrict
    .map(StompMessage.unmarshallImpl)


  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/ws"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => {

          val (outActor, outSourcePreMat) = outSource.preMaterialize()

          val in : Sink[Message, _] = inFlow
            .to(
              Sink.actorRefWithAck(
                actorRef(outActor),
                WSActorServer.Init,
                WSActorServer.Ack,
                WSActorServer.Complete) )

          upgrade.handleMessagesWithSinkSource(in, outSourcePreMat, Some("v12.stomp"))
        }
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  lazy val wsRoutes: Route =
    path("ws") {
      handleWith(requestHandler)
    }

}
