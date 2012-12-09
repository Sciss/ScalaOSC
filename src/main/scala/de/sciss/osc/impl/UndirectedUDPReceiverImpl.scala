package de.sciss.osc
package impl

import java.nio.channels.DatagramChannel

private[osc] final class UndirectedUDPReceiverImpl( val channel: DatagramChannel,
                                                    protected val config: UDP.Config )
extends UndirectedNetReceiverImpl with UDPChannelImpl with Channel.UndirectedInput.Net {
   protected def receive() {
      buf.clear()
      val sender = channel.receive( buf )
      flipDecodeDispatch( sender )
   }
}

