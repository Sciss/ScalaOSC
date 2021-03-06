package de.sciss.osc

import java.io.IOException

object VariousTests {
//	def codec() {
//// NOTE: scalacheck doesn't seem to be compatible with
////		 scala 2.8 BETA, and i cannot get the sources
//// 		 to compile due to more stupid dependencies.
//// 	     so screw scalacheck for the moment....
///*
//		import _root_.org.scalacheck._
//		import _root_.org.scalacheck.Arbitrary._
//		import _root_.org.scalacheck.Prop._
//
//		var c: PacketCodec = null
//		val b = ByteBuffer.allocate( 8192 )
//		val str = arbitrary[String] suchThat (_.indexOf(0) == -1) // null-character not allowed
//		val strictArgGen = Gen.oneOf( arbitrary[Int], arbitrary[Float], str )
//		val strictListGen = Gen.listOf[Any]( strictArgGen )
//		val fatArgGen = Gen.oneOf( arbitrary[Int], arbitrary[Float], arbitrary[Long], arbitrary[Double], str )
//		val fatListGen = Gen.listOf[Any]( fatArgGen )
//// how do we limit the list size? no clue... seems to work nevertheless
////		val sizedGen = Gen.sized { size => (size < 100) ==> listGen }
//		val checka = (list: List[Any]) => {
//			val msg = Message( "/test", list:_* )
//			b.clear
//			msg.encode( c, b )
//			b.flip
//			val msgOut = c.decode( b ).asInstanceOf[Message]
//			val decArgs = msgOut.args
//			(msgOut.name == msg.name) :| "name" &&
//			(decArgs == msg.args) :| ("args before: " + msg.args + " / after: " + decArgs.toList )
//		}
//		c = new PacketCodec( PacketCodec.MODE_STRICT_V1 )
//		val strictProp = forAll( strictListGen )( check )
//		strictProp.check
//		c = new PacketCodec( PacketCodec.MODE_FAT_V1 )
//		val fatProp = forAll( fatListGen )( check )
//		fatProp.check
//*/
//	}

  def receiver(): Unit = {

    println( """
               |Receiver test
               |
               |   is waiting for an incoming message
               |   on UDP port 21327
               |   Send "/quit" to terminate.
               | """.stripMargin)

    val cfg       = UDP.Config()
    cfg.localPort = 21327 // 0x53 0x4F or 'SO'
    val rcv       = UDP.Receiver(cfg)

    val sync      = new AnyRef

    rcv.dump(Dump.Both)
    rcv.action = {
      case (Message(name, _ @ _ *), _) =>
      println("Received message '" + name + "'")
      if (name == "/quit") sync.synchronized(sync.notifyAll())
      case (p, addr) => println("Ignoring: " + p + " from " + addr)
    }
    rcv.connect()
    sync.synchronized(sync.wait())
  }

  def transmitter(transport: Transport): Unit = {
    import Implicits._

    println(
      """Transmitter tests
        |   assume that scsynth is running on
        |   localhost """ + transport.name + """ port 57110
                                               | """.stripMargin)

    val tgt   = localhost -> 57110
    val trns  = transport match {
      case UDP => UDP.Transmitter(tgt)
      case TCP => TCP.Transmitter(tgt)
    }

    try {
      trns.dump(stream = Console.out)
      trns.connect()
      //println( trns.target )
      trns ! Message("/s_new", "default", 1000, 0, 0, "amp", 0f)
      for (i <- 1 to 8) {
        trns ! Message("/n_set", 1000, "freq", i * 333, "amp", 0.5f)
        Thread.sleep(200)
      }

      import de.sciss.osc.{Message => M}
      trns ! M("/n_free", 1000)

    } catch {
      case _: InterruptedException => ()
      case e2: IOException =>
        println(e2.getClass.getName + " : " + e2.getLocalizedMessage)
    }
    finally {
      trns.close()
    }
  }

  //   def client( transport: )

  def tcpClient(): Unit = {
    import Implicits._
    //      val c = TCP.Client( "127.0.0.1" -> 57110 )
    val c = TCP.Client(localhost -> 57110)
    c.connect()
    c.dump()
    c ! Message("/dumpOSC", 1)
    c ! Message("/notify", 1)
    c.action = p => {
      println(p)
      c ! Message("/dumpOSC", 0)
      sys.exit(0)
    }
  }

  def tcpServer(): Unit = {
    val server = TCP.Server()
    server.action = {
      case (Message(_, args @ _*), from) =>
        from ! Message("/pong", args: _*)
    }
    server.connect()

    val client = TCP.Client(server.localSocketAddress)
    client.dump()
    client.connect()
    client ! Message("/ping", 1, 2.3f)

    Thread.sleep(1000)
    server.close()
    client.close()
    sys.exit(0)
  }

  def pingPong(): Unit = {
    import de.sciss.osc
    val pingT = osc.UDP.Transmitter() // unidirectional, not bound
    val pingR = osc.UDP.Receiver(pingT.channel) // unidirectional, not bound, same channel
    val pong  = osc.UDP.Client  (pingT.localSocketAddress) // bidirectional, bound
    pingT.connect()
    pingR.connect()
    pong .connect()
    println("Ping at " + pingT.localSocketAddress)
    println("Pong at " + pong.localSocketAddress)

    val t = new java.util.Timer()
    def delay(code: => Unit): Unit =
      t.schedule(new java.util.TimerTask {
        def run(): Unit = code
      }, 500)

    pingR.action = {
      case (m @ osc.Message("/ping", cnt: Int), sender) =>
        println("Ping received " + m)
        delay {
          pingT.send(osc.Message("/pong", cnt), sender)
        }
      case _ =>
    }
    var cnt = 0
    pong.action = packet => {
      println("Pong received " + packet)
      cnt += 1
      if (cnt <= 10) {
        delay {
          pong ! osc.Message("/ping", cnt)
        }
      } else {
        pingR.close()
        pingT.close()
        pong .close()
        sys.exit(0)
      }
    }
    pingT.send(osc.Message("/start"), pong.localSocketAddress)
  }
}