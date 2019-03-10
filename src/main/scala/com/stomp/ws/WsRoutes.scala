package com.stomp.ws

import akka.actor.{ Actor, ActorSystem }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage }
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

  lazy val wsRoutes: Route =
    path("ws") {
      handleWebSocketMessages(ws)
    }

}
