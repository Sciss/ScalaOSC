///*
// * TCPReceiver.scala
// * (ScalaOSC)
// *
// * Copyright (c) 2008-2011 Hanns Holger Rutz. All rights reserved.
// *
// * This library is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 2.1 of the License, or (at your option) any later version.
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this library; if not, write to the Free Software
// * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
// *
// *
// * For further information, please contact Hanns Holger Rutz at
// * contact@sciss.de
// */
//
//package de.sciss.osc
//package impl
//
//import java.net.InetSocketAddress
//import java.nio.channels.{InterruptibleChannel, SocketChannel}
//
//final class TCPReceiver private( _localAddress: InetSocketAddress, sch: SocketChannel, val config: TCP.Config )
//extends OSCReceiver {
//   def transport = config.transport
//
//   private val sender = sch.socket().getRemoteSocketAddress
//
//   protected def connectChannel() { sys.error( "TODO" )}
//
//   protected def channel : InterruptibleChannel = sch
//
//   protected def receive() {
//      buf.rewind().limit( 4 )	// in TCP mode, first four bytes are packet size in bytes
//      do {
//         val len = sch.read( buf )
//         if( len == -1 ) return
//      } while( buf.hasRemaining )
//
//      buf.rewind()
//      val packetSize = buf.getInt()
//      buf.rewind().limit( packetSize )
//
//      while( buf.hasRemaining ) {
//         val len = sch.read( buf )
//         if( len == -1 ) return
//      }
//
//      flipDecodeDispatch( sender )
//   }
//}