package de.sciss.osc
package impl

import java.nio.channels.DatagramChannel
import java.net.SocketAddress

private[osc] final class DirectedUDPReceiverImpl( val channel: DatagramChannel,
                                                  protected val target: SocketAddress,
                                                  protected val config: UDP.Config )
extends DirectedReceiverImpl with UDPChannelImpl {
   def isConnected = channel.isConnected

   protected def connectChannel() {
      if( isConnected ) return
      channel.connect( target )
   }

   protected def receive() {
      buf.clear()
      channel.receive( buf )
      flipDecodeDispatch()
   }
}