/*
 * Client.scala
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

object Client {
  type Net = Client with Channel.Net.ConfigLike

  def apply(target: SocketAddress, config: Channel.Net.Config): Client.Net = config match {
    case udp: UDP     .Config => UDP    .Client(target, udp )
    case tcp: TCP     .Config => TCP    .Client(target, tcp )
    case _ => throw new IllegalArgumentException(s"Unsupported config $config")
  }
}