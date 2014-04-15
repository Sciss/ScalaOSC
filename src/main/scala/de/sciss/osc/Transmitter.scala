/*
 * Transmitter.scala
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

import java.net.SocketAddress

object Transmitter {
  type Directed = Channel.Directed.Output
  type Net      = Channel.Net

  object Undirected {
    trait Net extends Channel with Channel.Net.ConfigLike {
      def send(p: Packet, target: SocketAddress): Unit
    }
  }

  object Directed {
    type Net = Transmitter.Directed with Channel.Directed.Net
  }

  // convenient redirection

  def apply(target: SocketAddress, config: Channel.Net.Config): Transmitter.Directed.Net = config match {
    case udp: UDP.Config => UDP.Transmitter(target, udp)
    case tcp: TCP.Config => TCP.Transmitter(target, tcp)
  }
}
