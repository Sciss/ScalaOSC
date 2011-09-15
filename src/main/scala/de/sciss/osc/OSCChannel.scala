/*
 * OSCChannel.scala
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

import java.io.{ IOException, PrintStream }
import java.nio.channels.Channel
import java.net.{InetAddress, InetSocketAddress, SocketAddress}

object OSCChannel {
	/**
	 *	The default buffer size (in bytes) and maximum OSC packet
	 *	size (8K at the moment).
	 */
	val DEFAULTBUFSIZE = 8192
	
	val PassAllPackets : OSCPacket => Boolean = _ => true

   type Net = OSCChannel with NetConfigLike
//   trait Net extends OSCChannel with OSCChannelNetConfigLike

   trait DirectedNet extends OSCChannel with NetConfigLike {
      /**
       * The remote socket address of this channel. Returns `null` if the
       * channel has not yet been connected.
       *
       * @see  #connect()
       */
      def remoteSocketAddress : InetSocketAddress
      final def remotePort    : Int          = remoteSocketAddress.getPort
      final def remoteAddress : InetAddress  = remoteSocketAddress.getAddress
   }

/* sealed */ trait ConfigLike {
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
   trait Config extends ConfigLike

   trait NetConfigLike extends ConfigLike {
      override def transport : OSCTransport.Net

      def localSocketAddress : InetSocketAddress

      final def localPort        : Int          = localSocketAddress.getPort
      final def localAddress     : InetAddress  = localSocketAddress.getAddress
      final def localIsLoopback  : Boolean      = localSocketAddress.getAddress.isLoopbackAddress
   }

   trait NetConfig extends Config with NetConfigLike

   trait ConfigBuilder extends ConfigLike {
      def bufferSize_=( size: Int ) : Unit
      def codec_=( codec: OSCPacketCodec ) : Unit
      def build : Config
   }

   trait NetConfigBuilder extends ConfigBuilder with NetConfigLike {
      def localPort_=( port: Int ) : Unit
      def localAddress_=( address: InetAddress ) : Unit
      def localIsLoopback_=( loopback: Boolean ) : Unit

      override def build : NetConfig
   }

   private[osc] trait ConfigBuilderImpl extends ConfigBuilder {
      final var bufferSize             = 8192
      final var codec : OSCPacketCodec = OSCPacketCodec.default
   }

   private[osc] trait NetConfigBuilderImpl
   extends ConfigBuilderImpl with NetConfigBuilder {
      private var localSocket       = new InetSocketAddress( 0 )
      final def localSocketAddress  = localSocket

      final def localPort_=( port: Int ) {
         localSocket = new InetSocketAddress( localSocket.getAddress, port )
      }

      final def localAddress_=( address: InetAddress ) {
         localSocket = new InetSocketAddress( address, localSocket.getPort )
      }

      final def localIsLoopback_=( loopback: Boolean ) {
         if( localSocket.getAddress.isLoopbackAddress != loopback ) {
            localAddress = InetAddress.getByName( if( loopback ) "127.0.0.1" else "0.0.0.0" )
         }
      }
   }
}

import OSCChannel._

trait OSCChannel extends OSCChannel.ConfigLike with Channel {
   @volatile protected var dumpMode: OSCDump = OSCDump.Off
   @volatile protected var printStream : PrintStream	= Console.err
   @volatile protected var dumpFilter : (OSCPacket) => Boolean = PassAllPackets

   protected def config : OSCChannel.Config

//   final def transport : OSCTransport = config.transport
   final def bufferSize : Int = config.bufferSize
   final def codec : OSCPacketCodec = config.codec

	/**
	 *	Changes the way processed OSC messages are printed to the standard err console.
	 *	By default messages are not printed.
	 *
	 *  @param	mode	one of `OSCDump.Off` (don't dump, default),
	 *					`OSCDump.Text` (dump human readable string),
	 *					`OSCDump.Hex` (hexdump), or
	 *					`OSCDump.Both` (both text and hex)
	 *	@param	stream	the stream to print on
	 */
	def dumpOSC( mode: OSCDump = OSCDump.Text,
				    stream: PrintStream = Console.err,
				    filter: (OSCPacket) => Boolean = PassAllPackets ) {
		dumpMode	   = mode
		printStream	= stream
		dumpFilter	= filter
	}

	/**
	 *	Disposes the resources associated with the OSC communicator.
	 *	The object should not be used any more after calling this method.
	 */
	def close() : Unit
}

trait OSCInputChannel
extends OSCChannel {
	def action_=( f: (OSCMessage, SocketAddress, Long) => Unit )
	def action: (OSCMessage, SocketAddress, Long) => Unit

	/**
	 *	Changes the way incoming messages are dumped
	 *	to the console. By default incoming messages are not
	 *	dumped. Incoming messages are those received
	 *	by the client from the server, before they
	 *	get delivered to registered <code>OSCListener</code>s.
	 *
	 *	@param	mode	see <code>dumpOSC( int )</code> for details
	 *	@param	stream	the stream to print on
	 *
	 *	@see	#dumpOSC( int, PrintStream )
	 *	@see	#dumpOutgoingOSC( int, PrintStream )
	 */
	def dumpIncomingOSC( mode: OSCDump = OSCDump.Text,
					     stream: PrintStream = Console.err,
					     filter: (OSCPacket) => Boolean = PassAllPackets )
}

trait OSCOutputChannel
extends OSCChannel {
	/**
	 *	Changes the way outgoing messages are dumped
	 *	to the console. By default outgoing messages are not
	 *	dumped. Outgoing messages are those send via
	 *	<code>send</code>.
	 *
	 *	@param	mode	see <code>dumpOSC( int )</code> for details
	 *	@param	stream	the stream to print on
	 *
	 *	@see	#dumpOSC( int, PrintStream )
	 *	@see	#dumpIncomingOSC( int, PrintStream )
	 */
	def dumpOutgoingOSC( mode: OSCDump = OSCDump.Text,
						      stream: PrintStream = Console.err,
					         filter: (OSCPacket) => Boolean = PassAllPackets )
}
