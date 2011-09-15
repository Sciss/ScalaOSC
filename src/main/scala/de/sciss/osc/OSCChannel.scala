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
}

import OSCChannel._

trait OSCChannel extends OSCChannelConfigLike with Channel {
   @volatile protected var dumpMode: OSCDump = OSCDump.Off
   @volatile protected var printStream : PrintStream	= Console.err
   @volatile protected var dumpFilter : (OSCPacket) => Boolean = PassAllPackets

   protected def config : OSCChannelConfig

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

trait OSCChannelNet extends OSCChannel with OSCChannelNetConfigLike {
   /**
    *	Queries the communicator's local socket address.
    *	You can determine the host and port from the returned address
    *	by calling <code>getHostName()</code> (or for the IP <code>getAddress().getHostAddress()</code>)
    *	and <code>getPort()</code>.
    *
    *	@return				the address of the communicator's local socket.
    *
    *	@see	java.net.InetSocketAddress#getHostName()
    *	@see	java.net.InetSocketAddress#getAddress()
    *	@see	java.net.InetSocketAddress#getPort()
    *
    *	@see	#getProtocol()
    */
   @throws( classOf[ IOException ])
   def localSocketAddress : InetSocketAddress

//   final def localPort        : Int          = localSocketAddress.getPort
//   final def localAddress     : InetAddress  = localSocketAddress.getAddress
//   final def localIsLoopback  : Boolean      = localSocketAddress.getAddress.isLoopbackAddress
}

trait OSCInputChannel
extends OSCChannel {
	def action_=( f: (OSCMessage, SocketAddress, Long) => Unit )
	def action: (OSCMessage, SocketAddress, Long) => Unit
	
//	/**
//	 *	Starts the communicator.
//	 *
//	 *	@throws	IOException	if a networking error occurs
//	 */
//	@throws( classOf[ IOException ])
//	def start() : Unit

//	/**
//	 *	Checks whether the communicator is active (was started) or not (is stopped).
//	 *
//	 *	@return	<code>true</code> if the communicator is active, <code>false</code> otherwise
//	 */
//	def isActive : Boolean

//	/**
//	 *	Stops the communicator.
//	 *
//	 *	@throws	IOException	if a networking error occurs
//	 */
//	@throws( classOf[ IOException ])
//	def stop() : Unit

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
