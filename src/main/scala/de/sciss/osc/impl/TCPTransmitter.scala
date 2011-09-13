/*
 * TCPTransmitter.scala
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

import java.net.{SocketAddress, InetSocketAddress}
import java.nio.BufferOverflowException
import java.nio.channels.{SelectableChannel, SocketChannel}
import java.io.IOException

final class TCPTransmitter private( channel: SocketChannel, val config: TCP.Config )
extends OSCTransmitter {

   def target: SocketAddress = channel.socket().getRemoteSocketAddress

//   def this( localAddress: InetSocketAddress, codec: OSCPacketCodec ) = this( localAddress, null, codec )

//   def this( sch: SocketChannel, config: TCP.Config ) {
//      this( new InetSocketAddress( sch.socket().getLocalAddress, sch.socket().getLocalPort ), sch, config,
//         sch.socket().getRemoteSocketAddress )
////      if( sch.isConnected ) target = sch.socket().getRemoteSocketAddress
//   }

   def localSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
   }

//   def localAddress : InetSocketAddress = {
//      sync.synchronized {
//         if( sch != null ) {
//            val s = sch.socket()
//            new InetSocketAddress( s.getLocalAddress, s.getLocalPort )
//         } else {
//            addr
//         }
//      }
//   }

//   private[ osc ] def channel : SelectableChannel = sch
//   {
//      sync.synchronized {
//         sch
//      }
//   }

//   @throws( classOf[ IOException ])
//   def connect() {
//      sync.synchronized {
//         if( (sch != null) && !sch.isOpen ) {
//            if( !revivable ) throw new IOException( "Channel cannot be revived" )
//            sch = null;
//         }
//         if( sch == null ) {
//            val newCh = SocketChannel.open()
//            newCh.socket().bind( addr )
//            sch = newCh
//         }
//         if( !sch.isConnected ) {
//            sch.connect( target )
//         }
//      }
//   }

//   def isConnected : Boolean = {
//      sync.synchronized {
//         (sch != null) && sch.isConnected
//      }
//   }

   @throws( classOf[ IOException ])
   protected def closeChannel() {
      channel.close()
   }

//   @throws( classOf[ IOException ])
//   def send( c: OSCPacketCodec, p: OSCPacket, target: SocketAddress ) {
//      sync.synchronized {
//         if( (target != null) && (target != this.target) )
//            throw new IllegalStateException( "Not bound to address : " + target )
//
//         send( p, target )
//      }
//   }

   @throws( classOf[ IOException ])
   def send( p: OSCPacket, target: SocketAddress ) {
      try {
         generalSync.synchronized {
//            if( sch == null ) throw new IOException( "Channel not connected" );
//            checkBuffer()
            byteBuf.clear()
            byteBuf.position( 4 )
            p.encode( codec, byteBuf )
            val len = byteBuf.position() - 4
            byteBuf.flip()
            byteBuf.putInt( 0, len )
            dumpPacket( p )
            channel.write( byteBuf )
         }
      }
      catch { case e: BufferOverflowException =>
          throw new OSCException( OSCException.BUFFER,
             if( p.isInstanceOf[ OSCMessage ]) p.asInstanceOf[ OSCMessage ].name else p.getClass.getName )
      }
   }
}