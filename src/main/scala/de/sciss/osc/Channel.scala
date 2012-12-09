/*
 * Channel.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2012 Hanns Holger Rutz. All rights reserved.
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
import java.nio.channels.{InterruptibleChannel, Channel => NIOChannel}
import java.net.{SocketAddress, InetAddress, InetSocketAddress}

object Channel {
   trait ConfigLike {
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

   trait ConfigBuilder extends ConfigLike {
      /**
       * Adjusts the buffer size used by the future channel.
       * The minimum allowed size is 16 bytes. Typically, OSC
       * applications handle packets up to 8 KB. SuperCollider
       * Server handles packets up the 64 KB by default (?).
       */
      def bufferSize_=( size: Int ) : Unit
      def codec_=( codec: PacketCodec ) : Unit
      def build : Config
   }

   object Net {
      trait ConfigLike extends Channel.ConfigLike {
         override def transport : Transport.Net

         def localSocketAddress : InetSocketAddress

         final def localPort        : Int          = localSocketAddress.getPort
         final def localAddress     : InetAddress  = localSocketAddress.getAddress
         final def localIsLoopback  : Boolean      = localSocketAddress.getAddress.isLoopbackAddress
      }

      trait Config extends Channel.Config with ConfigLike

      trait ConfigBuilder extends Channel.ConfigBuilder with ConfigLike {
         def localPort_=( port: Int ) : Unit
         def localAddress_=( address: InetAddress ) : Unit
         def localSocketAddress_=( address: InetSocketAddress ) : Unit
         def localIsLoopback_=( loopback: Boolean ) : Unit

         override def build : Config
      }
   }

   type Net = Channel with Net.ConfigLike

   trait DirectedNet extends Channel with Net.ConfigLike {
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

   object DirectedInput {
      type Action = Packet => Unit
      val NoAction : Action = _ => ()
   }
   trait DirectedInput extends Channel {
      def action : DirectedInput.Action
      def action_=( fun: DirectedInput.Action ) : Unit
   }

   object UndirectedInput {
      object Net {
         type Action = (Packet, SocketAddress) => Unit
         val NoAction : Action = (_, _) => ()
      }
      trait Net extends Channel {
         def action: Net.Action
         def action_=( value: Net.Action ) : Unit
      }
   }

   trait DirectedOutput extends Channel /* extends OutputLike */ {
      def !( p: Packet ) : Unit
   }

   trait Bidi {
      def dumpIn(  mode: Dump = Dump.Text,
                   stream: PrintStream = Console.err,
                   filter: Dump.Filter = Dump.AllPackets ) : Unit

      def dumpOut( mode: Dump = Dump.Text,
                   stream: PrintStream = Console.err,
                   filter: Dump.Filter = Dump.AllPackets ) : Unit
   }
}

trait Channel extends Channel.ConfigLike with NIOChannel {
   def bufferSize : Int
   def codec : PacketCodec

   def channel: InterruptibleChannel

   /**
    *	Queries whether the channel is still open.
    */
   def isOpen : Boolean

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

   def isConnected: Boolean

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
	def dump( mode: Dump = Dump.Text,
				 stream: PrintStream = Console.err,
				 filter: Dump.Filter = Dump.AllPackets ) : Unit
}