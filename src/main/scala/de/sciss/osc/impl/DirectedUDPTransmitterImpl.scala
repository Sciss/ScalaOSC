package de.sciss.osc
package impl

import java.nio.channels.DatagramChannel
import java.net.SocketAddress
import java.io.IOException

private[osc] final class DirectedUDPTransmitterImpl( val channel: DatagramChannel,
                                                     protected val target: SocketAddress,
                                                     protected val config: UDP.Config )
extends UDPTransmitterImpl with Channel.Directed.Output with Channel.Directed.Net {
   override def toString = transport.name + ".Transmitter@" + hashCode().toHexString

   @throws( classOf[ IOException ])
   protected def connectChannel() { if( !isConnected ) channel.connect( target )}
   def isConnected = channel.isConnected

   @throws( classOf[ IOException ])
   def !( p: Packet ) {
      bufSync.synchronized {
         buf.clear()
         p.encode( codec, buf )
         buf.flip()
         dumpPacket( p )
         channel.write( buf )
      }
   }
}
