/*
 * Receiver.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2021 Hanns Holger Rutz. All rights reserved.
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
  type Net      = Channel with Channel.Net.ConfigLike
  type Directed = Channel.Directed.Input

  object Directed {
    type Net = Receiver.Directed with Receiver.Net
  }

  object Undirected {
    @deprecated("Will be removed", since = "1.2.4")
    type Action = (Packet, SocketAddress) => Unit
    @deprecated("Will be removed", since = "1.2.4")
    val NoAction: Action = (_, _) => ()

    val  Net = Channel.Undirected.Input.Net
    type Net = Channel.Undirected.Input.Net
  }

   // convenient redirection

  def apply(target: SocketAddress, config: Channel.Net.Config): Receiver.Directed.Net = config match {
    case udp: UDP     .Config => UDP    .Receiver(target, udp )
    case tcp: TCP     .Config => TCP    .Receiver(target, tcp )
    case _ => throw new IllegalArgumentException(s"Unsupported config $config")
  }
}