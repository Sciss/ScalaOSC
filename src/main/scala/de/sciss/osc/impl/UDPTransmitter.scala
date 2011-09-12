/*
 * UDPTransmitter.scala
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

package de.sciss.osc.impl

import java.nio.BufferOverflowException
import de.sciss.osc._
import java.net.{SocketAddress, InetSocketAddress}
import java.io.IOException
import java.nio.channels.{SelectableChannel, DatagramChannel}

final class UDPTransmitter( _addr: InetSocketAddress, private var dch: DatagramChannel, var codec: OSCPacketCodec )
extends OSCTransmitter( UDP, _addr, dch == null ) {

//  private var dch: DatagramChannel = null

	def this( localAddress: InetSocketAddress, codec: OSCPacketCodec ) = this( localAddress, null, codec )
	def this( dch: DatagramChannel, codec: OSCPacketCodec ) {
		this( new InetSocketAddress( dch.socket.getLocalAddress, dch.socket.getLocalPort ), dch, codec )
  	}

 	private[ osc ] def channel : SelectableChannel = {
		sync.synchronized {
			dch
		}
	}

	def localAddress : InetSocketAddress = {
		sync.synchronized {
			if( dch != null ) {
				val ds = dch.socket
				new InetSocketAddress( ds.getLocalAddress, ds.getLocalPort )
			} else {
//				localAddress
				addr
			}
		}
	}

	@throws( classOf[ IOException ])
	def connect() {
		sync.synchronized {
			if( (dch != null) && !dch.isOpen ) {
				if( !revivable ) throw new IOException( "Channel cannot be revived" )
				dch = null
			}
			if( dch == null ) {
				val newCh = DatagramChannel.open()
				newCh.socket.bind( addr )
				dch = newCh
			}
		}
	}

	def isConnected : Boolean = {
		sync.synchronized {
			(dch != null) && dch.isOpen
		}
	}

	override def dispose() {
		super.dispose()
		if( dch != null ) {
			try {
				dch.close()
			}
			catch { case e: IOException => /* ignored */ }
			dch = null
		}
	}

   @throws( classOf[ IOException ])
   def send( p: OSCPacket, target: SocketAddress ) {
      try {
         sync.synchronized {
            if( dch == null ) throw new IOException( "Channel not connected" );
            checkBuffer()
            byteBuf.clear()
            p.encode( codec, byteBuf )
            byteBuf.flip()
            dumpPacket( p )
            dch.send( byteBuf, target )
         }
      }
      catch { case e: BufferOverflowException =>
          throw new OSCException( OSCException.BUFFER,
             if( p.isInstanceOf[ OSCMessage ]) p.asInstanceOf[ OSCMessage ].name else p.getClass.getName )
      }
   }
}