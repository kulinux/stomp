package com.stomp.ws

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage, UpgradeToWebSocket }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
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
        implicit val mat = ActorMaterializer()
        bm.dataStream.runWith(Sink.ignore)
        ???
      }
      case unknow => {
        println("unknow message")
        ???
      }

    }

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/ws"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessages(ws, Some("v12.stomp"))
        case None => HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case r: HttpRequest =>
      implicit val mat = ActorMaterializer()
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      HttpResponse(404, entity = "Unknown resource!")
  }

  lazy val wsRoutes: Route =
    path("ws") {
      //handleWebSocketMessages(ws)
      handleWith(requestHandler)
    }

}
