package de.sciss.osc
package impl

import java.nio.channels.SocketChannel
import java.net.SocketAddress
import java.io.IOException

private[osc] final class TCPTransmitterImpl( val channel: SocketChannel,
                                             protected val target: SocketAddress,
                                             protected val config: TCP.Config )
extends TransmitterImpl with TCPSingleChannelImpl with Channel.Directed.Output {
   override def toString = TCP.name + ".Transmitter(" + target + ")@" + hashCode().toHexString

   def isConnected = channel.isConnected

   @throws( classOf[ IOException ])
   def !( p: Packet ) {
      // config ensures that buf size is at least 16!
//            if( buf.limit < 4 ) throw PacketCodec.BufferOverflow( p.name )
      bufSync.synchronized {
         buf.clear()
         buf.position( 4 )
         p.encode( codec, buf )
         val len = buf.position() - 4
         buf.flip()
         buf.putInt( 0, len )
         dumpPacket( p )
         channel.write( buf )
      }
   }
}
