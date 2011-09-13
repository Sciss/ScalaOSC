/*
 * OSCTransport.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2011 Hanns Holger Rutz. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */
package de.sciss.osc

import java.net.{InetAddress, InetSocketAddress}

sealed trait OSCTransport { def name: String }
sealed trait OSCNetTransport extends OSCTransport

case object UDP extends OSCNetTransport {
   val name = "UDP"

   def config : ConfigBuilder = new ConfigBuilderImpl

   sealed trait Config extends OSCChannelNetConfig
   sealed trait ConfigBuilder extends OSCChannelNetConfigBuilder {
      override def build : Config
   }

   private final class ConfigBuilderImpl extends OSCChannelNetConfigBuilderImpl with ConfigBuilder {
      def transport = UDP
      def build: Config = ConfigImpl( bufferSize, codec, localSocketAddress )
   }

   private final case class ConfigImpl( bufferSize: Int, codec: OSCPacketCodec,
                                        localSocketAddress: InetSocketAddress )
   extends Config {
      def transport = UDP
   }
}

case object TCP extends OSCNetTransport {
   val name = "TCP"

   sealed trait Config extends OSCChannelNetConfig
}

case object OSCFileTransport extends OSCTransport {
   val name = "File"
}

/* sealed */ trait OSCChannelConfigLike {
   /**
    *	Queries the buffer size used for coding or decoding OSC messages.
    *	This is the maximum size an OSC packet (bundle or message) can grow to.
    *
    *	@return			the buffer size in bytes.
    *
    *	@see	#setBufferSize( int )
    */
   def bufferSize : Int

   /**
    *	Queries the transport protocol used by this communicator.
    *
    *	@return	the transport, such as <code>UDP</code> or <code>TCP</code>
    *
    *	@see	#UDP
    *	@see	#TCP
	 */
   def transport : OSCTransport

   def codec : OSCPacketCodec

}
sealed trait OSCChannelConfig extends OSCChannelConfigLike

sealed trait OSCChannelNetConfigLike extends OSCChannelConfigLike {
   override def transport : OSCNetTransport

   def localSocketAddress : InetSocketAddress

   final def localPort        : Int          = localSocketAddress.getPort
   final def localAddress     : InetAddress  = localSocketAddress.getAddress
   final def localIsLoopback  : Boolean      = localSocketAddress.getAddress.isLoopbackAddress
}

sealed trait OSCChannelNetConfig extends OSCChannelConfig with OSCChannelNetConfigLike

sealed trait OSCChannelConfigBuilder extends OSCChannelConfigLike {
   def bufferSize_=( size: Int ) : Unit
   def codec_=( codec: OSCPacketCodec ) : Unit
   def build : OSCChannelConfig
}

sealed trait OSCChannelNetConfigBuilder extends OSCChannelConfigBuilder with OSCChannelNetConfigLike {
   def localPort_=( port: Int ) : Unit
   def localAddress_=( address: InetAddress ) : Unit
   def localIsLoopback_=( loopback: Boolean ) : Unit

   override def build : OSCChannelNetConfig
}

private[osc] sealed trait OSCChannelConfigBuilderImpl extends OSCChannelConfigBuilder {
   var bufferSize                = 8192
   var codec : OSCPacketCodec    = OSCPacketCodec.default
}

private[osc] sealed trait OSCChannelNetConfigBuilderImpl
extends OSCChannelConfigBuilderImpl with OSCChannelNetConfigBuilder {
   private var localSocket       = new InetSocketAddress( 0 )
   def localSocketAddress        = localSocket

   def localPort_=( port: Int ) {
      localSocket = new InetSocketAddress( localSocket.getAddress, port )
   }

   def localAddress_=( address: InetAddress ) {
      localSocket = new InetSocketAddress( address, localSocket.getPort )
   }

   def localIsLoopback_=( loopback: Boolean ) {
      if( localSocket.getAddress.isLoopbackAddress != loopback ) {
         localAddress = InetAddress.getByName( if( loopback ) "127.0.0.1" else "0.0.0.0" )
      }
   }
}
