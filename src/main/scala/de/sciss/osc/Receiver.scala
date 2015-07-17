/*
 * Receiver.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2015 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc

import java.net.SocketAddress

object Receiver {
  type Net      = Channel /* Receiver */ with Channel.Net.ConfigLike
  type Directed = Channel.Directed.Input

  object Directed {
    type Net = Receiver.Directed with Receiver.Net
  }

  object Undirected {
    type Action = (Packet, SocketAddress) => Unit
    val NoAction: Action = (_, _) => ()

    val  Net = Channel.Undirected.Input.Net
    type Net = Channel.Undirected.Input.Net
  }

   // convenient redirection

  def apply(target: SocketAddress, config: Channel.Net.Config): Receiver.Directed.Net = config match {
    case udp: UDP.Config => UDP.Receiver(target, udp)
    case tcp: TCP.Config => TCP.Receiver(target, tcp)
  }
}