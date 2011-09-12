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
import java.net.{ InetSocketAddress, SocketAddress }

object OSCChannel {
	/**
	 *	Dump mode: do not dump messages
	 */
	val DUMP_OFF		= 0
	/**
	 *	Dump mode: dump messages in text formatting
	 */
	val DUMP_TEXT		= 1
	/**
	 *	Dump mode: dump messages in hex (binary) view
	 */
	val DUMP_HEX		= 2
	/**
	 *	Dump mode: dump messages both in text and hex view
	 */
	val DUMP_BOTH		= 3
	
	/**
	 *	The default buffer size (in bytes) and maximum OSC packet
	 *	size (8K at the moment).
	 */
	val DEFAULTBUFSIZE = 8192
	
	private[osc] def NO_FILTER( p: OSCPacket ) = true
}

import OSCChannel._

/**
 *    @version 0.11, 27-May-10
 */
trait OSCChannel {
    protected var dumpMode					= DUMP_OFF
    protected var printStream : PrintStream	= null
    protected var dumpFilter : (OSCPacket) => Boolean = NO_FILTER
	
	/**
	 *	Queries the transport protocol used by this communicator.
	 *	
	 *	@return	the transport, such as <code>UDP</code> or <code>TCP</code>
	 *
	 *	@see	#UDP
	 *	@see	#TCP
	 */
	def transport : OSCTransport

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
	def localAddress : InetSocketAddress
	
	/**
	 *	Adjusts the buffer size for OSC messages.
	 *	This is the maximum size an OSC packet (bundle or message) can grow to.
	 *
	 *	@param	size					the new size in bytes.
	 *
	 *	@see	#getBufferSize()
	 */
	def bufferSize_=( size: Int ) : Unit

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
	 *	Changes the way processed OSC messages are printed to the standard err console.
	 *	By default messages are not printed.
	 *
	 *  @param	mode	one of <code>kDumpOff</code> (don't dump, default),
	 *					<code>kDumpText</code> (dump human readable string),
	 *					<code>kDumpHex</code> (hexdump), or
	 *					<code>kDumpBoth</code> (both text and hex)
	 *	@param	stream	the stream to print on, or <code>null</code> which
	 *					is shorthand for <code>System.err</code>
	 *
	 *	@see	#DUMP_OFF
	 *	@see	#DUMP_TEXT
	 *	@see	#DUMP_HEX
	 *	@see	#DUMP_BOTH
	 */
	def dumpOSC( mode: Int = DUMP_TEXT,
				 stream: PrintStream = System.err,
				 filter: (OSCPacket) => Boolean = NO_FILTER ) {
		dumpMode	= mode
		printStream	= stream
		dumpFilter	= filter
	}

	/**
	 *	Disposes the resources associated with the OSC communicator.
	 *	The object should not be used any more after calling this method.
	 */
	def dispose() : Unit
	
	def codec : OSCPacketCodec
	def codec_=( c: OSCPacketCodec ) : Unit
}

trait OSCInputChannel
extends OSCChannel {
	def action_=( f: (OSCMessage, SocketAddress, Long) => Unit )
	def action: (OSCMessage, SocketAddress, Long) => Unit
	
	/**
	 *	Starts the communicator.
	 *
	 *	@throws	IOException	if a networking error occurs
	 */
	@throws( classOf[ IOException ])
	def start() : Unit

	/**
	 *	Checks whether the communicator is active (was started) or not (is stopped).
	 *
	 *	@return	<code>true</code> if the communicator is active, <code>false</code> otherwise
	 */
	def isActive : Boolean

	/**
	 *	Stops the communicator.
	 *
	 *	@throws	IOException	if a networking error occurs
	 */
	@throws( classOf[ IOException ])
	def stop() : Unit

	/**
	 *	Changes the way incoming messages are dumped
	 *	to the console. By default incoming messages are not
	 *	dumped. Incoming messages are those received
	 *	by the client from the server, before they
	 *	get delivered to registered <code>OSCListener</code>s.
	 *
	 *	@param	mode	see <code>dumpOSC( int )</code> for details
	 *	@param	stream	the stream to print on, or <code>null</code> which
	 *					is shorthand for <code>System.err</code>
	 *
	 *	@see	#dumpOSC( int, PrintStream )
	 *	@see	#dumpOutgoingOSC( int, PrintStream )
	 */
	def dumpIncomingOSC( mode: Int = DUMP_TEXT,
					     stream: PrintStream = System.err,
					     filter: (OSCPacket) => Boolean = NO_FILTER )
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
	 *	@param	stream	the stream to print on, or <code>null</code> which
	 *					is shorthand for <code>System.err</code>
	 *
	 *	@see	#dumpOSC( int, PrintStream )
	 *	@see	#dumpIncomingOSC( int, PrintStream )
	 */
	def dumpOutgoingOSC( mode: Int = DUMP_TEXT,
						 stream: PrintStream = System.err,
					     filter: (OSCPacket) => Boolean = NO_FILTER )
}