/*
 * UDPReceiver.scala
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

import java.nio.channels.{ClosedChannelException, SelectableChannel, DatagramChannel}
import java.net.{DatagramPacket, DatagramSocket, SocketAddress, InetSocketAddress}
import java.io.IOException

final class UDPReceiver( _addr: InetSocketAddress, dch: DatagramChannel, val config: UDP.Config )
extends OSCReceiver( _addr ) {
//	def this( localAddress: InetSocketAddress, codec: OSCPacketCodec ) = this( localAddress, null, codec )
	def this( dch: DatagramChannel, config: UDP.Config ) {
		this( new InetSocketAddress( dch.socket.getLocalAddress, dch.socket.getLocalPort ), dch, config )
	}

   // ---- constructor ----
//   require( _c.transport == UDP )

//	def codec : OSCPacketCodec = c
//	def codec_=( cdc: OSCPacketCodec ) {
//		c = cdc
//	}

   def transport = config.transport

//	@throws( classOf[ IOException ])
//	private[ osc ] def channel_=( ch: SelectableChannel ) {
//		generalSync.synchronized {
//			if( listening ) throw new IllegalStateException( "Cannot be called while receiver is active" )
//
//			val dchTmp	= ch.asInstanceOf[ DatagramChannel ]
//			if( !dchTmp.isBlocking ) {
//				dchTmp.configureBlocking( true )
//			}
//			if( dchTmp.isConnected ) throw new IllegalStateException( "Channel is not connected" )
//			dch = dchTmp
//		}
//	}
	private[ osc ] def channel : SelectableChannel = dch

//	def localSocketAddress : InetSocketAddress = {
////		generalSync.synchronized {
////			if( dch != null ) {
//				val ds = dch.socket()
//				getLocalAddress( ds.getLocalAddress, ds.getLocalPort )
////			} else {
////				getLocalAddress( addr.getAddress, addr.getPort )
////			}
////		}
//	}

	def target_=( t: SocketAddress ) {
		tgt = t
	}

//	@throws( classOf[ IOException ])
//	def connect() {
//		generalSync.synchronized {
//			if( listening ) throw new IllegalStateException( "Cannot be called while receiver is active" )
//
//			if( (dch != null) && !dch.isOpen ) {
//				if( !revivable ) throw new IOException( "Channel cannot be revived" )
//				dch = null
//			}
//			if( dch == null ) {
//				val newCh = DatagramChannel.open()
//				newCh.socket.bind( localAddress )
//				channel = newCh
//			}
//		}
//	}
//
//	def isConnected : Boolean = {
//		generalSync.synchronized {
//			(dch != null) && dch.isOpen
//		}
//	}

	@throws( classOf[ IOException ])
	protected def closeChannel() {
//		if( dch != null ) {
//			try {
				dch.close()
//			}
//			finally {
//				dch = null
//			}
//		}
	}

	/**
	 *	This is the body of the listening thread
	 */
	protected def receiverLoop() {
      while( isOpenNoSync ) {
         try {
            byteBuf.clear()
//println( "in run : " + dch )
            val sender = dch.receive( byteBuf )

            if( isOpenNoSync && (sender != null) &&
               ((tgt == null) || tgt.equals( sender ))) {

               flipDecodeDispatch( sender )
            }
         }
         catch {
            case e1: ClosedChannelException =>  // bye bye, we have to quit
               if( isOpenNoSync ) {
                  Console.err.println( "OSCReceiver.run : " + e1.getClass.getName + " : " + e1.getLocalizedMessage )
               }
               return

            case e2: IOException =>
               if( isOpenNoSync ) {
                  Console.err.println( "OSCReceiver.run : " + e2.getClass.getName + " : " + e2.getLocalizedMessage )
               }
         }
      } // while( listening )
   }

	@throws( classOf[ IOException ])
	protected def sendGuardSignal() {
		val guard		   = new DatagramSocket
		val guardPacket	= new DatagramPacket( new Array[ Byte ]( 0 ), 0 )
		guardPacket.setSocketAddress( localSocketAddress )
		guard.send( guardPacket )
		guard.close()
	}
}
