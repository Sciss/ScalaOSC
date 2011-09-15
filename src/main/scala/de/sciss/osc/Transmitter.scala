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
import java.net.SocketAddress

object Transmitter {
   type Directed = Transmitter with Channel.DirectedOutput
//   trait Directed extends Transmitter {
//      def !( p: Packet ) : Unit
//   }

   type Net = Transmitter with Channel.Net

   trait UndirectedNet extends Transmitter with Channel.NetConfigLike { // Channel.Net
      def send( p: Packet, target: SocketAddress ) : Unit

      @throws( classOf[ IOException ])
      protected final def connectChannel() {}  // XXX or: if( !isOpen ) throw new ChannelClosedException ?
      final def isConnected = isOpen
   }

   type DirectedNet = Directed with Channel.DirectedNet
}

trait Transmitter extends Channel.Output {
   @throws( classOf[ IOException ])
   final def close() {
      channel.close()
   }

   @throws( classOf[ IOException ])
   final def connect() {
      connectChannel()
   }
}