package de.sciss.osc
package impl

import java.nio.channels.DatagramChannel
import java.net.SocketAddress

private[osc] final class UDPClientImpl( val channel: DatagramChannel,
                                        protected val target: SocketAddress,
                                        protected val config: UDP.Config )
extends ClientImpl with UDPChannelImpl {
   protected val input  = {
      UDP.Receiver(    channel, target, config )
//      new impl.UndirectedUDPTransmitterImpl( channel, config )
   }
   protected val output = {
      UDP.Transmitter( channel, target, config )
//      new impl.DirectedUDPTransmitterImpl( channel, target, config )
   }
}
