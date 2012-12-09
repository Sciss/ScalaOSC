package de.sciss.osc

object TestApp extends App {
   args.toSeq match {
      case Seq( "--pingPong" ) =>
         VariousTests.pingPong()
      case Seq( "--testTransmitter", transName ) =>
         Transport( transName ) match {
            case netTrans: Transport.Net => VariousTests.transmitter( netTrans )
            case _ => sys.error( "Unsupported transport '" + transName + "'" )
         }
      case Seq( "--testReceiver" ) =>
         VariousTests.receiver()
//         case Seq( "--runChecks" ) =>
//            Test.codec()
      case Seq( "--testTCPClient" ) =>
         VariousTests.tcpClient()
      case Seq( "--testTCPServer" ) =>
         VariousTests.tcpServer()
      case _ =>
//         printInfo()
         println(
            """The following demos are available:
              |
              |--pingPong
              |--testTransmitter (UDP|TCP)
              |--testReceiver
              |--testTCPClient
              |--testTCPServer
              |""".stripMargin
         )
         sys.exit( 1 )
   }
}
