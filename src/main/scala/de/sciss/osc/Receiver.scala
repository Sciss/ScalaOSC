/*
 * Receiver.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2013 Hanns Holger Rutz. All rights reserved.
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
import java.nio.channels.{SocketChannel, DatagramChannel}

object Receiver {
   type Net       = Channel /* Receiver */ with Channel.Net.ConfigLike
   type Directed  = Channel.Directed.Input
   object Directed {
      type Net = Receiver.Directed with Receiver.Net
   }

   object Undirected {
      type Action = (Packet, SocketAddress) => Unit
      val NoAction : Action = (_, _) => ()

      val Net  = Channel.Undirected.Input.Net
      type Net = Channel.Undirected.Input.Net
   }

   // convenient redirection

//   def apply( config: UDP.Config ) : UDP.Receiver.Undirected = UDP.Receiver( config )
//   def apply( channel: DatagramChannel ) : UDP.Receiver.Undirected = UDP.Receiver( channel )
//   def apply( channel: DatagramChannel, config: UDP.Config ) : UDP.Receiver.Undirected = UDP.Receiver( channel, config )

//   def apply( target: SocketAddress, config: UDP.Config ) : UDP.Receiver.Directed = UDP.Receiver( target, config )
//   def apply( channel: DatagramChannel, target: SocketAddress ) : UDP.Receiver.Directed = UDP.Receiver( channel, target )
//   def apply( channel: DatagramChannel, target: SocketAddress, config: UDP.Config ) : UDP.Receiver.Directed =
//      UDP.Receiver( channel, target, config )

//   def apply( target: SocketAddress, config: TCP.Config ) : TCP.Receiver = TCP.Receiver( target, config )
//   def apply( channel: SocketChannel, target: SocketAddress ) : TCP.Receiver = TCP.Receiver( channel, target )
//   def apply( channel: SocketChannel, target: SocketAddress, config: TCP.Config ) : TCP.Receiver =
//      TCP.Receiver( channel, target, config )

   def apply( target: SocketAddress, config: Channel.Net.Config ) : Receiver.Directed.Net = config match {
      case udp: UDP.Config => UDP.Receiver( target, udp )
      case tcp: TCP.Config => TCP.Receiver( target, tcp )
   }
}