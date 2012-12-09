package de.sciss.osc
package impl

import java.net.{InetAddress, InetSocketAddress}
import java.nio.channels.{ServerSocketChannel, SocketChannel}

private[osc] final class TCPConfigBuilderImpl extends NetChannelConfigBuilderImpl with TCP.ConfigBuilder {
   def transport = TCP
   def build: TCP.Config = TCPConfigImpl( bufferSize, codec, localSocketAddress )
}

private[osc] final case class TCPConfigImpl( bufferSize: Int, codec: PacketCodec,
                                             localSocketAddress: InetSocketAddress )
extends TCP.Config {
   def transport = TCP

   def openChannel( discardWildcard: Boolean ) = {
      val ch      = SocketChannel.open()
      val addr    = localAddress( discardWildcard )
      ch.socket().bind( addr )
      ch
   }

   private def localAddress( discardWildcard: Boolean ) : InetSocketAddress = {
      val addr0 = localSocketAddress
      if( discardWildcard && addr0.getAddress
            .equals( InetAddress.getByAddress( new Array[ Byte ]( 4 )))) {
         new InetSocketAddress( InetAddress.getLocalHost, addr0.getPort )
      } else addr0
   }

   def openServerChannel( discardWildcard: Boolean ) = {
      val ch      = ServerSocketChannel.open()
      val addr    = localAddress( discardWildcard )
      ch.socket().bind( addr )
      ch
   }
}

