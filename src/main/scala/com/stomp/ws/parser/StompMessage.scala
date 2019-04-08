package com.stomp.ws.parser

import akka.stream.scaladsl.Flow

case class StompMessage(
  command: String,
  header: Map[String, String] = Map(),
  body: String = "")

object StompMessage {

  val Connect = "CONNECT"
  val Connected = "CONNECTED"
  val Send = "SEND"
  val Message = "MESSAGE"

  val NULL: Char = 0

  def unmarshallImpl(str: String): StompMessage = {
    val withoutNull = str.init
    val lines = withoutNull.split("\n")
    if (lines.size < 1) throw new RuntimeException("StompMessage unmarshallImpl error")
    val command = lines.head

    val headers = lines.tail
      .takeWhile(_.isEmpty == false)
      .map(_.split(":"))
      .map(header => header.head -> header.tail.head)
      .toMap

    val bodyLines = lines
      .dropWhile(line => line.isEmpty == false)

    val body = if (bodyLines.isEmpty) ""
    else bodyLines.tail.mkString("\n")

    StompMessage(command, headers, body)
  }

  def marshallImpl(message: StompMessage) = {
    val res = message.command + "\n" +
      message.header
      .map(hd => hd._1 + ":" + hd._2)
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

  val marshall: Flow[StompMessage, String, _] =
    Flow[StompMessage].map(sm => marshallImpl(sm))

}
