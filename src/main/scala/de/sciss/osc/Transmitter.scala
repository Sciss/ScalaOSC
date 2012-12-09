/*
 * Transmitter.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2012 Hanns Holger Rutz. All rights reserved.
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

import java.net.SocketAddress

object Transmitter {
   type Directed  = Channel.Directed.Output
   type Net       = Channel.Net

   object Undirected {
      trait Net extends Channel with Channel.Net.ConfigLike { // Channel.Net
         def send( p: Packet, target: SocketAddress ) : Unit
      }
   }

   object Directed {
      type Net = Transmitter.Directed with Channel.Directed.Net
   }

   // convenient redirections

//   def apply( config: UDP.Config ) : UDP.Transmitter.Undirected = UDP.Transmitter( config )
//   def apply( target: SocketAddress, config: TCP.Config ) : TCP.Transmitter = TCP.Transmitter( target, config )
//   def apply( target: SocketAddress, config: UDP.Config ) : UDP.Transmitter.Directed = UDP.Transmitter( target, config )
   def apply( target: SocketAddress, config: Channel.Net.Config ) : Transmitter.Directed.Net = config match {
      case udp: UDP.Config => UDP.Transmitter( target, udp )
      case tcp: TCP.Config => TCP.Transmitter( target, tcp )
   }
}
