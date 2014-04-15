/*
 * Implicits.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2014 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc

import java.net.{InetAddress, InetSocketAddress}
import language.implicitConversions

object Implicits {
  implicit def stringToInetAddress       (host: String)             = InetAddress.getByName(host)
  implicit def stringTupleToSocketAddress(tup: (String, Int))       = new InetSocketAddress(tup._1, tup._2)
  implicit def addrTupleToSocketAddress  (tup: (InetAddress, Int))  = new InetSocketAddress(tup._1, tup._2)

  def localhost: InetAddress = InetAddress.getLocalHost
}
