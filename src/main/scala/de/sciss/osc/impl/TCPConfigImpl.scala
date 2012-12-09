package de.sciss.osc
package impl

import java.net.{InetAddress, InetSocketAddress}
import java.nio.channels.SocketChannel

private[osc] final class TCPConfigBuilderImpl extends NetChannelConfigBuilderImpl with TCP.ConfigBuilder {
   def transport = TCP
   def build: TCP.Config = TCPConfigImpl( bufferSize, codec, localSocketAddress )
}

private[osc] final case class TCPConfigImpl( bufferSize: Int, codec: PacketCodec,
                                             localSocketAddress: InetSocketAddress )
extends TCP.Config {
   def transport = TCP
   // XXX factor out common parts with UDP.ConfigImpl
   def openChannel( discardWildcard: Boolean ) = {
      val ch      = SocketChannel.open()
      val addr0   = localSocketAddress
      val addr    = if( discardWildcard && addr0.getAddress
            .equals( InetAddress.getByAddress( new Array[ Byte ]( 4 )))) {
         new InetSocketAddress( InetAddress.getLocalHost, addr0.getPort )
      } else addr0

      ch.socket().bind( addr )
      ch
   }
//      def openChannel() = {
//         val ch = SocketChannel.open()
//         ch.socket().bind( localSocketAddress )
//         ch
//      }
}

