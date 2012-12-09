package de.sciss.osc
package impl

import java.net.{InetAddress, InetSocketAddress}
import java.nio.channels.DatagramChannel

private[osc] final class UDPConfigBuilderImpl extends NetChannelConfigBuilderImpl with UDP.ConfigBuilder {
   def transport = UDP
   def build: UDP.Config = UDPConfigImpl( bufferSize, codec, localSocketAddress )
}

private[osc] final case class UDPConfigImpl( bufferSize: Int, codec: PacketCodec,
                                             localSocketAddress: InetSocketAddress )
extends UDP.Config {
   def transport = UDP
   def openChannel( discardWildcard: Boolean ) = {
      val ch      = DatagramChannel.open()
      val addr0   = localSocketAddress
      val addr    = if( discardWildcard && addr0.getAddress
            .equals( InetAddress.getByAddress( new Array[ Byte ]( 4 )))) {
         new InetSocketAddress( InetAddress.getLocalHost, addr0.getPort )
      } else addr0

      ch.socket().bind( addr )
      ch
   }
}
