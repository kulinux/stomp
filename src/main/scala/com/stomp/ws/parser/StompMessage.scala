package com.stomp.ws.parser

import akka.stream.scaladsl.Flow

case class StompMessage(
  command: String,
  header: Seq[(String, String)],
  body: String)

object StompMessage {
  def parse(str: String) = {
    StompMessage("", Seq(), "")

  }
  val unmarshall: Flow[String, StompMessage, _] =
    Flow[String].map(parse(_))

  val process: Flow[StompMessage, StompMessage, _] =
    Flow[StompMessage].map(sm => StompMessage("", Seq(), ""))

  val marshall: Flow[StompMessage, String, _] = Flow[StompMessage].map(sm => "Vuelta")

}
