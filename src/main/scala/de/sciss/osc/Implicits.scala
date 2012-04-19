/*
 * Implicits.scala
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

import java.net.{InetAddress, InetSocketAddress}

object Implicits {
   implicit def stringToInetAddress( host: String )                  = InetAddress.getByName( host )
   implicit def stringTupleToSocketAddress( tup: (String, Int) )     = new InetSocketAddress( tup._1, tup._2 )
   implicit def addrTupleToSocketAddress( tup: (InetAddress, Int) )  = new InetSocketAddress( tup._1, tup._2 )
   def localhost : InetAddress = InetAddress.getLocalHost
}
