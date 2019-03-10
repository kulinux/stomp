package com.stomp.ws

import akka.actor.{ ActorSystem }
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage, UpgradeToWebSocket }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink, Source }

trait WsRoutes {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  def ws: Flow[Message, Message, Any] = Flow[Message].mapConcat {
    case tm: TextMessage =>
      TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!!!")) :: Nil
    case bm: BinaryMessage => {
      implicit val mat = ActorMaterializer()
      bm.dataStream.runWith(Sink.ignore)
      Nil
    }
  }

  val requestHandler: HttpRequest => HttpResponse = {
    case req @ HttpRequest(HttpMethods.GET, Uri.Path("/ws"), _, _, _) =>
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) => upgrade.handleMessages(ws)
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
