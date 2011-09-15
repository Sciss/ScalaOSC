/*
 * OSCTransmitter.scala
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

import java.io.IOException
import java.nio.channels.{InterruptibleChannel, SocketChannel}
import java.nio.ByteBuffer
import java.net.{InetAddress, InetSocketAddress, SocketAddress}

object OSCTransmitter {
   trait Directed extends OSCTransmitter {
      def !( p: OSCPacket ) : Unit
   }

   type Net = OSCTransmitter with OSCChannelNet

//   trait TCP extends DirectedNet {
//      override protected def config: TCP.Config
//      override protected def channel: SocketChannel
//
////      final def target = channel.socket().getRemoteSocketAddress
//
//      final def transport = config.transport
//
//      final def localSocketAddress = {
//         val so = channel.socket()
//         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
//      }
//   }

   trait UndirectedNet extends OSCTransmitter with OSCChannelNet {
      def send( p: OSCPacket, target: SocketAddress ) : Unit

      @throws( classOf[ IOException ])
      def connect() {}
      def isConnected = isOpen
   }

   trait DirectedNet extends OSCTransmitter with OSCChannelNet with Directed {
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
}

trait OSCTransmitter extends OSCChannel {
	protected final val bufSync	= new AnyRef
	protected final val byteBuf   = ByteBuffer.allocateDirect( config.bufferSize )

	/**
	 *	Establishes connection for transports requiring
	 *	connectivity (e.g. TCP). For transports that do not require connectivity (e.g. UDP),
	 *	this ensures the communication channel is created and bound.
	 *  <P>
	 *	When a <B>UDP</B> transmitter
	 *	is created without an explicit <code>DatagramChannel</code> &ndash; say by
	 *	calling <code>OSCTransmitter.newUsing( &quot;udp&quot; )</code>, you are required
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
	 *						and an <code>OSCReceiver</code> sharing the same socket was stopped before (unable to revive).
	 *
	 *	@see	#isConnected()
	 */
	@throws( classOf[ IOException ])
	def connect() : Unit

   @throws( classOf[ IOException ])
   final def close() {
      channel.close()
   }

   protected def channel: InterruptibleChannel

   /**
    *	Queries whether the <code>OSCReceiver</code> is
    *	listening or not.
    */
   final def isOpen : Boolean = channel.isOpen // generalSync.synchronized { !wasClosed }

	/**
	 *	Queries the connection state of the transmitter.
	 *
	 *	@return	<code>true</code> if the transmitter is connected, <code>false</code> otherwise. For transports that do not use
	 *			connectivity (e.g. UDP) this returns <code>false</code>, if the
	 *			underlying <code>DatagramChannel</code> has not yet been created.
	 *
	 *	@see	#connect()
	 */
	def isConnected : Boolean

   /**
    * Callers should have a lock on the buffer!
    */
   protected final def dumpPacket( p: OSCPacket ) {
      if( (dumpMode ne OSCDump.Off) && dumpFilter( p )) {
         printStream.synchronized {
            printStream.print( "s: " )
            dumpMode match {
               case OSCDump.Text =>
                  OSCPacket.printTextOn( codec, printStream, p )
               case OSCDump.Hex =>
                  OSCPacket.printHexOn( printStream, byteBuf )
               case OSCDump.Both =>
                  OSCPacket.printTextOn( codec, printStream, p )
                  OSCPacket.printHexOn( printStream, byteBuf )
               case _ =>   // satisfy compiler
            }
         }
      }
   }
}