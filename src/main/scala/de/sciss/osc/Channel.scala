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
import java.net.{InetAddress, InetSocketAddress}
import java.nio.channels.{InterruptibleChannel, Channel => NIOChannel}
import java.nio.ByteBuffer

object Channel {
//	/**
//	 *	The default buffer size (in bytes) and maximum OSC packet
//	 *	size (8K at the moment).
//	 */
//	val DEFAULTBUFSIZE = 8192
	
	val PassAllPackets : Packet => Boolean = _ => true

   type Net = Channel with NetConfigLike
//   trait Net extends Channel with OSCChannelNetConfigLike

   trait DirectedNet extends Channel with NetConfigLike {
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
      def transport : Transport

      def codec : PacketCodec

   }
   trait Config extends ConfigLike

   trait NetConfigLike extends ConfigLike {
      override def transport : Transport.Net

      def localSocketAddress : InetSocketAddress

      final def localPort        : Int          = localSocketAddress.getPort
      final def localAddress     : InetAddress  = localSocketAddress.getAddress
      final def localIsLoopback  : Boolean      = localSocketAddress.getAddress.isLoopbackAddress
   }

   trait NetConfig extends Config with NetConfigLike

   trait ConfigBuilder extends ConfigLike {
      def bufferSize_=( size: Int ) : Unit
      def codec_=( codec: PacketCodec ) : Unit
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
      final var codec : PacketCodec = PacketCodec.default
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

   private[osc] trait Single extends Channel {
      @volatile protected var dumpMode: Dump = Dump.Off
      @volatile protected var printStream : PrintStream	= Console.err
      @volatile protected var dumpFilter : (Packet) => Boolean = PassAllPackets

      protected val bufSync   = new AnyRef
      protected final val buf	= ByteBuffer.allocateDirect( config.bufferSize )

      final def dumpOSC( mode: Dump = Dump.Text,
                         stream: PrintStream = Console.err,
                         filter: (Packet) => Boolean = PassAllPackets ) {
         dumpMode	   = mode
         printStream	= stream
         dumpFilter	= filter
      }

      /**
       * Callers should have a lock on the buffer!
       */
      protected final def dumpPacket( p: Packet, prefix: String ) {
         if( (dumpMode ne Dump.Off) && dumpFilter( p )) {
            printStream.synchronized {
               printStream.print( prefix )
               dumpMode match {
                  case Dump.Text =>
                     Packet.printTextOn( codec, printStream, p )
                  case Dump.Hex =>
                     Packet.printHexOn( printStream, buf )
                  case Dump.Both =>
                     Packet.printTextOn( codec, printStream, p )
                     Packet.printHexOn( printStream, buf )
                  case _ =>   // satisfy compiler
               }
            }
         }
      }
   }

   private[osc] trait Output extends Single {
      protected def dumpPacket( p: Packet ) { dumpPacket( p, "s: " )}
   }

   private[osc] trait Input extends Single {
      protected def dumpPacket( p: Packet ) { dumpPacket( p, "r: " )}
   }

   trait Bidi extends Channel {
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
      def dumpIncomingOSC( mode: Dump = Dump.Text,
                       stream: PrintStream = Console.err,
                       filter: (Packet) => Boolean = PassAllPackets ) : Unit

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
      def dumpOutgoingOSC( mode: Dump = Dump.Text,
                           stream: PrintStream = Console.err,
                           filter: (Packet) => Boolean = PassAllPackets ) : Unit
   }
}

import Channel._

trait Channel extends Channel.ConfigLike with NIOChannel {
   protected def config : Channel.Config

   final def bufferSize : Int = config.bufferSize
   final def codec : PacketCodec = config.codec

   protected def channel: InterruptibleChannel

   /**
    *	Queries whether the channel is still open.
    */
   final def isOpen : Boolean = channel.isOpen // generalSync.synchronized { !wasClosed }

   /**
    *	Establishes connection for transports requiring
    *	connectivity (e.g. TCP). For transports that do not require connectivity (e.g. UDP),
    *	this ensures the communication channel is created and bound.
    *  <P>
    *	When a <B>UDP</B> transmitter
    *	is created without an explicit <code>DatagramChannel</code> &ndash; say by
    *	calling <code>Transmitter.newUsing( &quot;udp&quot; )</code>, you are required
    *	to call <code>connect()</code> so that an actual <code>DatagramChannel</code> is
    *	created and bound. For a <B>UDP</B> transmitter which was created with an explicit
    *	<code>DatagramChannel</code>, this method does noting, so it is always safe
    *	to call <code>connect()</code>. However, for <B>TCP</B> transmitters,
    *	this may throw an <code>IOException</code> if the transmitter
    *	was already connected, therefore be sure to check <code>isConnected()</code> before.
    *
    *	@throws	IOException	if a networking error occurs. Possible reasons: - the underlying
    *						network channel had been closed by the server. - the transport
    *						is TCP and the server is not available. - the transport is TCP
    *						and an <code>Receiver</code> sharing the same socket was stopped before (unable to revive).
    *
    *	@see	#isConnected()
    */
   @throws( classOf[ IOException ])
   def connect() : Unit

	/**
	 *	Changes the way processed OSC messages are printed to the standard err console.
	 *	By default messages are not printed.
	 *
	 *  @param	mode	one of `Dump.Off` (don't dump, default),
	 *					`Dump.Text` (dump human readable string),
	 *					`Dump.Hex` (hexdump), or
	 *					`Dump.Both` (both text and hex)
	 *	@param	stream	the stream to print on
	 */
	def dumpOSC( mode: Dump = Dump.Text,
				    stream: PrintStream = Console.err,
				    filter: (Packet) => Boolean = PassAllPackets ) : Unit
}