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

package de.sciss.osc
package impl

import java.nio.BufferOverflowException
import java.net.{SocketAddress, InetSocketAddress}
import java.io.IOException
import java.nio.channels.DatagramChannel

final class UDPTransmitter( channel: DatagramChannel, val config: UDP.Config )
extends OSCTransmitter {

//  private var dch: DatagramChannel = null

//	def this( localAddress: InetSocketAddress, codec: OSCPacketCodec ) = this( localAddress, null, codec )
//	def this( dch: DatagramChannel, config: UDP.Config ) {
//		this( new InetSocketAddress( dch.socket.getLocalAddress, dch.socket.getLocalPort ), dch, config )
//  	}

   def transport = config.transport

   def localSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
   }

   def target : SocketAddress = sys.error( "TODO" )

// 	private[ osc ] def channel : SelectableChannel = dch
//    {
//		sync.synchronized {
//			dch
//		}
//	}

//	def localAddress : InetSocketAddress = {
//		sync.synchronized {
//			if( dch != null ) {
//				val ds = dch.socket
//				new InetSocketAddress( ds.getLocalAddress, ds.getLocalPort )
//			} else {
////				localAddress
//				addr
//			}
//		}
//	}

//	@throws( classOf[ IOException ])
//	def connect() {
//		sync.synchronized {
//			if( (dch != null) && !dch.isOpen ) {
//				if( !revivable ) throw new IOException( "Channel cannot be revived" )
//				dch = null
//			}
//			if( dch == null ) {
//				val newCh = DatagramChannel.open()
//				newCh.socket.bind( addr )
//				dch = newCh
//			}
//		}
//	}

//	def isConnected : Boolean = {
//		sync.synchronized {
//			(dch != null) && dch.isOpen
//		}
//	}

   @throws( classOf[ IOException ])
	protected def closeChannel() {
      channel.close()
	}

   @throws( classOf[ IOException ])
   def send( p: OSCPacket, target: SocketAddress ) {
      try {
         generalSync.synchronized {
//            if( dch == null ) throw new IOException( "Channel not connected" );
//            checkBuffer()
            byteBuf.clear()
            p.encode( codec, byteBuf )
            byteBuf.flip()
            dumpPacket( p )
            channel.send( byteBuf, target )
         }
      }
      catch { case e: BufferOverflowException =>
          throw new OSCException( OSCException.BUFFER, p match {
             case m: OSCMessage => m.name
             case _ => p.getClass.getName
          })
      }
   }
}