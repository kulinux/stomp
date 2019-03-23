package com.stomp.ws.parser

import akka.stream.scaladsl.Flow

case class StompMessage(
  command: String,
  header: Map[String, String],
  body: String)

object StompMessage {

  val NULL : Char = 0
  def unmarshallImpl(str: String): StompMessage = {
    val lines = str.split("\n")
    if(lines.size < 1) throw new RuntimeException("StompMessage unmarshallImpl error")
    val command = lines.head
    val headers = lines.tail
      .takeWhile(_.isEmpty == false)
      .map( _.split(":") )
      .map( header => header.head -> header.tail.head)
      .toMap

    val body = lines
      .dropWhile(_.isEmpty == false)
      .init
      .tail.mkString("\n")

    StompMessage(command, headers, body)
  }

  def marshallImpl(message: StompMessage) = {
    val res = message.command + "\n" +
    message.header
      .map( hd => hd._1 + ":" + hd._2)
      .mkString("\n") +
    "\n" +
    "\n" +
    message.body +
    "\n" +
    NULL
    res
  }



  val unmarshall: Flow[String, StompMessage, _] =
    Flow[String].map(unmarshallImpl(_))

  val process: Flow[StompMessage, StompMessage, _] =
    Flow[StompMessage].map(sm => StompMessage("", Map(), ""))

  val marshall: Flow[StompMessage, String, _] = Flow[StompMessage].map(sm => "Vuelta")

}
