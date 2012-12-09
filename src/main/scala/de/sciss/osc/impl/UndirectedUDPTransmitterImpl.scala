package de.sciss.osc
package impl

import java.nio.channels.DatagramChannel
import de.sciss.osc.Transmitter
import java.io.IOException
import java.net.SocketAddress

private[osc] final class UndirectedUDPTransmitterImpl( val channel: DatagramChannel,
                                                       protected val config: UDP.Config )
extends UDPTransmitterImpl with Transmitter.Undirected.Net {
   override def toString = transport.name + ".Transmitter@" + hashCode().toHexString

//         def isConnected = channel.isOpen
//         protected def connectChannel() {}
//         def isConnected = isOpen

   @throws( classOf[ IOException ])
   def send( p: Packet, target: SocketAddress ) {
      bufSync.synchronized {
         buf.clear()
         p.encode( codec, buf )
         buf.flip()
         dumpPacket( p )
         channel.send( buf, target )
      }
   }

   @throws( classOf[ IOException ])
   protected def connectChannel() {}  // XXX or: if( !isOpen ) throw new ChannelClosedException ?
   def isConnected = isOpen
}
