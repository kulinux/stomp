package com.stomp.ws.parser

import akka.stream.scaladsl.Flow

case class StompMessage(
  command: String,
  header: Map[String, String] = Map(),
  body: String = "")

object StompMessage {

  val CONNECT = "CONNECT"
  val CONNECTED = "CONNECTED"
  val SEND = "SEND"

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

  def connect(stm: StompMessage): StompMessage =
    StompMessage(
      "CONNECTED",
      Map(
        "version" -> "1.2",
        "heart-beat" -> "0,0",
        "server" -> "StompScala/1.0"))

  def send(stm: StompMessage) = {
    println(s"SEND $stm")

  }

  val unmarshall: Flow[String, StompMessage, _] =
    Flow[String].map(unmarshallImpl(_))

  val process: Flow[StompMessage, Option[StompMessage], _] =
    Flow[StompMessage].map(sm => {
      sm.command match {
        case StompMessage.CONNECT => Some(connect(sm))
        case StompMessage.SEND => { send(sm); None }
        case _ => Some(StompMessage("ERROR", Map(), s"Unknow command ${sm.command}"))
      }
    })

  val marshall: Flow[StompMessage, String, _] = Flow[StompMessage].map(sm => marshallImpl(sm))

}
