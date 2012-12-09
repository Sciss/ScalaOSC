/*
 * Receiver.scala
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

object Receiver {
   type Net       = Channel /* Receiver */ with Channel.NetConfigLike
   type Directed  = Channel.DirectedInput

   type DirectedNet = Directed with Net

   object Undirected {
      type Action = (Packet, SocketAddress) => Unit
      val NoAction : Action = (_, _) => ()
   }

   val UndirectedNet    = Channel.UndirectedNetInput
   type UndirectedNet   = Channel.UndirectedNetInput
}