package de.sciss.osc
package impl

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

private[osc] trait TCPChannelImpl extends ChannelImpl {
   protected def config: TCP.Config
   final def transport = config.transport
}

private[osc] trait TCPSingleChannelImpl extends TCPChannelImpl with TCP.Channel with DirectedImpl {
   override def channel: SocketChannel
   override protected def config: TCP.Config

   final def localSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
   }

   final protected def connectChannel() { if( !channel.isConnected ) channel.connect( target )}

   final def remoteSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getInetAddress, so.getPort )
   }
}
