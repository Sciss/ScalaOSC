package de.sciss.osc
package impl

import java.net.SocketAddress

private[osc] trait DirectedImpl extends ChannelImpl {
   protected def target: SocketAddress
}