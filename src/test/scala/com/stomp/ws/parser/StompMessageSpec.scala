package com.stomp.ws.parser

import org.scalatest.{FlatSpec, Matchers}

class StompMessageSpec extends FlatSpec with Matchers {
  val NULL: Char = 0
  val stompMsgStr = """Connect
                     |login:login
                     |passcode:pwd
                     |accept-version:1.2,1.1,1.0
                     |heart-beat:10000,10000
                     |
                     |Este es body
                     |""".stripMargin + NULL

  val stompMsg = StompMessage("CONNECT",
      Map(
        "login" -> "login",
        "passcode" -> "pwd",
        "accept-version" -> "1.2,1.1,1.0",
        "heart-beat" -> "10000,10000"
      ),
      "Este es body")

  "A message" should "unmarshall" in {
    StompMessage.unmarshallImpl(stompMsgStr) should be ( stompMsg )
  }

  "A message" should "marshall" in {
    StompMessage.marshallImpl(stompMsg) should be ( stompMsgStr )
  }

}
