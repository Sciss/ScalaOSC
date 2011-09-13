/*
 * TCPReceiver.scala
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

import java.io.IOException
import java.net.{SocketAddress, InetSocketAddress}
import java.nio.channels.{ClosedChannelException, AlreadyConnectedException, SelectableChannel, SocketChannel}
import de.sciss.osc.{OSCChannelConfig, OSCException, TCP, OSCReceiver, OSCPacketCodec}

final class TCPReceiver private( _localAddress: InetSocketAddress, sch: SocketChannel, val config: TCP.Config )
extends OSCReceiver( _localAddress ) {
//   def this( localAddress: InetSocketAddress, c: OSCChannelConfig ) =
//      this( localAddress, null, c )

   private val sender = sch.socket().getRemoteSocketAddress

   def this( sch: SocketChannel, config: TCP.Config ) =
      this( new InetSocketAddress( sch.socket().getLocalAddress, sch.socket().getLocalPort ), sch, config )

   // ---- constructor ----
//   require( _c.transport == TCP )

//   @throws( classOf[ IOException ])
//   private[ osc ] def channel_=( ch: SelectableChannel ) {
//      generalSync.synchronized {
//         if( listening ) throw new IllegalStateException( "Cannot be called while receiver is active" )
//
//         sch	= ch.asInstanceOf[ SocketChannel ]
//         if( !sch.isBlocking ) {
//            sch.configureBlocking( true )
//         }
//      }
//   }
   private[ osc ] def channel : SelectableChannel = sch

//   def localSocketAddress : InetSocketAddress = {
////      generalSync.synchronized {
////         if( sch != null ) {
//            val s = sch.socket()
//            getLocalAddress( s.getLocalAddress, s.getLocalPort )
////         } else {
////            getLocalAddress( addr.getAddress, addr.getPort )
////         }
////      }
//   }

//   def target_=( t: SocketAddress ) {
//      generalSync.synchronized {
//         if( isConnected ) throw new AlreadyConnectedException()
//         tgt = t
//      }
//   }
//
//   @throws( classOf[ IOException ])
//   def connect() {
//      generalSync.synchronized {
//         if( listening ) throw new IllegalStateException( "Cannot be called while receiver is active" )
//
//         if( (sch != null) && !sch.isOpen ) {
//            if( !revivable ) throw new IOException( "Channel cannot be revived" )
//            sch = null
//         }
//         if( sch == null ) {
//            val newCh = SocketChannel.open()
//            newCh.socket().bind( localAddress )
//            sch = newCh
//         }
//         if( !sch.isConnected ) {
//            sch.connect( target )
//         }
//      }
//   }
//
//   def isConnected : Boolean = {
//      generalSync.synchronized {
//         (sch != null) && sch.isConnected()
//      }
//   }

   @throws( classOf[ IOException ])
   protected def closeChannel() {
//      if( sch != null ) {
//         try {
            sch.close()
//         }
//         finally {
//            sch = null
//         }
//      }
   }

   protected def receiverLoop() {
//      val sender = sch.socket().getRemoteSocketAddress
//      checkBuffer()

      while( isOpenNoSync ) {
         try {
            byteBuf.rewind().limit( 4 )	// in TCP mode, first four bytes are packet size in bytes
            do {
               val len = sch.read( byteBuf )
               if( len == -1 ) return
            } while( byteBuf.hasRemaining )

            byteBuf.rewind()
            val packetSize = byteBuf.getInt()
            byteBuf.rewind().limit( packetSize )

            while( byteBuf.hasRemaining ) {
               val len = sch.read( byteBuf )
               if( len == -1 ) return
            }

            flipDecodeDispatch( sender )
         }
         catch {
            case e1: IllegalArgumentException =>	// thrown on illegal byteBuf.limit() calls
               if( isOpenNoSync ) {
                  val e2 = new OSCException( OSCException.RECEIVE, e1.toString )
                  Console.err.println( "OSCReceiver.run : " + e2.getClass.getName + " : " + e2.getLocalizedMessage )
               }
            case e1: ClosedChannelException =>	// bye bye, we have to quit
               if( isOpenNoSync ) {
                  Console.err.println( "OSCReceiver.run : " + e1.getClass.getName + " : " + e1.getLocalizedMessage )
               }
            case e1: IOException =>
               if( isOpenNoSync ) {
                  Console.err.println( "OSCReceiver.run : " + e1.getClass.getName + " : " + e1.getLocalizedMessage )
               }
         }
      }
   }

   /**
    *	@warning	this calls socket().shutdownInput()
    *				to unblock the listening thread. unfortunately this
    *				cannot be undone, so it's not possible to revive the
    *				receiver in TCP mode ;-( have to check for alternative ways
    */
   @throws( classOf[ IOException ])
   protected def sendGuardSignal() {
      sch.socket().shutdownInput()
   }
}