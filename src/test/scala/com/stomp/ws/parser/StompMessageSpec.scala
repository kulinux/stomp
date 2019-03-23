package com.stomp.ws.parser

import org.scalatest.{FlatSpec, Matchers}

class StompMessageSpec extends FlatSpec with Matchers {
  val connectMsg = """CONNECT
                     |login:login
                     |passcode:pwd
                     |accept-version:1.2,1.1,1.0
                     |heart-beat:10000,10000
                     |
                     | """

  "A connect message" should "parse" in {
    StompMessage.parse(connectMsg) should be (
      StompMessage("CONNECT", Seq(), "")
    )
  }

}
