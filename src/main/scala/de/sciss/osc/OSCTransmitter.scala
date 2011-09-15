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

   type Net = OSCTransmitter with OSCChannel.Net

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

   trait UndirectedNet extends OSCTransmitter with OSCChannel.NetConfigLike { // OSCChannel.Net
      def send( p: OSCPacket, target: SocketAddress ) : Unit

      @throws( classOf[ IOException ])
      def connect() {}
      def isConnected = isOpen
   }

   type DirectedNet = Directed with OSCChannel.DirectedNet
}

trait OSCTransmitter extends OSCChannel {
	protected final val bufSync	= new AnyRef
	protected final val buf       = ByteBuffer.allocateDirect( config.bufferSize )

   @throws( classOf[ IOException ])
   final def close() {
      channel.close()
   }

//   protected def channel: InterruptibleChannel

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
                  OSCPacket.printHexOn( printStream, buf )
               case OSCDump.Both =>
                  OSCPacket.printTextOn( codec, printStream, p )
                  OSCPacket.printHexOn( printStream, buf )
               case _ =>   // satisfy compiler
            }
         }
      }
   }
}