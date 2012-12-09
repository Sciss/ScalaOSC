package de.sciss.osc
package impl

import java.net.SocketAddress
import java.nio.channels.SocketChannel

private[osc] final class TCPClientImpl( val channel: SocketChannel,
                                        protected val target: SocketAddress,
                                        protected val config: TCP.Config )
extends ClientImpl with TCPSingleChannelImpl {
   protected val input  = TCP.Receiver( channel, target, config )
   protected val output = TCP.Transmitter( channel, target, config )
}
