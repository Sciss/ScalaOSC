package de.sciss.osc
package impl

import java.net.InetSocketAddress

private[osc] trait TCPChannelImpl extends DirectedImpl with TCP.Channel {
   override protected def config: TCP.Config
   final def transport = config.transport

   final def localSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
   }

   final protected def connectChannel() { if( !isConnected ) channel.connect( target )}

   final def remoteSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getInetAddress, so.getPort )
   }
}
