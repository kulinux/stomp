package com.stomp.ws

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.stomp.ws.parser.StompMessage

trait WsRoutes {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  def ws: Flow[Message, Message, Any] = Flow[Message]
    .map {
      case tm: TextMessage =>
        implicit val mat = ActorMaterializer()
        val res: Source[String, _] = tm.textStream
          .via(StompMessage.unmarshall)
          .via(StompMessage.process)
          .filter(p => p.isDefined)
          .map(p => p.get)
          .via(StompMessage.marshall)

        TextMessage(res)
      case bm: BinaryMessage => {
        println("binary message received")
        val mat = ActorMaterializer()
        bm.dataStream.runWith(Sink.ignore)(mat)
        ???
      }
      case unknow => {
        println("unknow message")
        ???
      }

    }

  lazy val inSink: Sink[Message, _] = Flow[Message]
    .filter(_.isText)
    .map(tm => tm.asTextMessage.getStrictText)
    .map(StompMessage.unmarshallImpl)
    .to(
      Sink.actorRefWithAck(actorRef, WSActor.Init, WSActor.Ack, WSActor.Complete))

  lazy val outSource: Source[StompMessage, ActorRef] =
    Source.actorRef(1, OverflowStrategy.fail)

  lazy val outActor: (ActorRef, Source[Message, _]) = outSource
    .map(st => StompMessage.marshallImpl(st))
    .map(str => TextMessage(str))
    .preMaterialize()(ActorMaterializer())

  /*
    Source
      .actorRef(10, OverflowStrategy.fail)
      .map(StompMessage.marshallImpl)
      .to(Sink.ignore)
      .run()(ActorMaterializer())
      */

  lazy val actorRef: ActorRef = system.actorOf(Props(new WSActor(outActor._1)))

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/ws"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessagesWithSinkSource(inSink, outActor._2, Some("v12.stomp"))
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      val actorMaterializer = ActorMaterializer()
      r.discardEntityBytes()(actorMaterializer) // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  lazy val wsRoutes: Route =
    path("ws") {
      //handleWebSocketMessages(ws)
      handleWith(requestHandler)
    }

}
