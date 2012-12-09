package de.sciss.osc
package impl

import java.io.{IOException, PrintStream}
import java.nio.ByteBuffer

private[osc] trait ChannelImpl extends Channel {
   protected def config : Channel.Config

   final def bufferSize : Int    = config.bufferSize
   final def codec : PacketCodec = config.codec
   final def isOpen : Boolean    = channel.isOpen
}

private[osc] trait SingleChannelImpl extends ChannelImpl {
   @volatile /* final */ protected var dumpMode: Dump = Dump.Off
   @volatile /* final */ protected var printStream : PrintStream	= Console.err
   @volatile /* final */ protected var dumpFilter : Dump.Filter = Dump.AllPackets

   final protected val bufSync   = new AnyRef
   final protected val buf	      = ByteBuffer.allocateDirect( config.bufferSize )

   /**
    * Requests to connect the network channel. This may be called several
    * times, and the implementation should ignore the call when the channel
    * is already connected.
    */
   @throws( classOf[ IOException ])
   protected def connectChannel() : Unit
//      protected def isChannelConnected : Boolean

   final def dump( mode: Dump, stream: PrintStream, filter: Dump.Filter ) {
      dumpMode	   = mode
      printStream	= stream
      dumpFilter	= filter
   }

   /**
    * Callers should have a lock on the buffer!
    */
   final protected def dumpPacket( p: Packet, prefix: String ) {
      if( (dumpMode ne Dump.Off) && dumpFilter( p )) {
         printStream.synchronized {
            printStream.print( prefix )
            dumpMode match {
               case Dump.Text =>
                  Packet.printTextOn( p, codec, printStream )
               case Dump.Hex =>
                  Packet.printHexOn( buf, printStream )
               case Dump.Both =>
                  Packet.printTextOn( p, codec, printStream )
                  Packet.printHexOn( buf, printStream )
               case _ =>   // satisfy compiler
            }
         }
      }
   }
}

private[osc] trait SingleOutputChannelImpl extends SingleChannelImpl {
   final protected def dumpPacket( p: Packet ) { dumpPacket( p, "s: " )}
}

private[osc] trait SingleInputChannelImpl extends SingleChannelImpl {
   final protected def dumpPacket( p: Packet ) { dumpPacket( p, "r: " )}
}