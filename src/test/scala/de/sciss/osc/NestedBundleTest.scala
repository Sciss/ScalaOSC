package de.sciss.osc

import java.nio.ByteBuffer

object NestedBundleTest extends App {
  run()

  def run(): Unit = {
    val m1   = Message("/1")
    val m2   = Message("/2")
    val m3   = Message("/3", Bundle.now(m1, m2))
    val b    = Bundle.now(m3)
    val bb   = ByteBuffer.allocate(8192)
    val c    = PacketCodec().packetsAsBlobs().build
    b.encode(c, bb)
    bb.flip()
    Packet.printHexOn(bb, Console.out)
    val res  = c.decode(bb)
    res match {
      case Bundle(Timetag.now, Message("/3", bb1: ByteBuffer)) =>
        c.decode(bb1) match {
          case Bundle(Timetag.now, `m1`, `m2`) =>
        }
    }
  }
}