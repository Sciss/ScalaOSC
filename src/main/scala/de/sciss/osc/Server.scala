/*
 * Server.scala
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

object Server {
  type Net = Server with Channel.Net.ConfigLike

  def apply(config: TCP.Config): Server.Net = TCP.Server(config)
}

trait Server extends Channel.Bidi {
  type Connection <: Channel.Directed.Output

  type Action = (Packet, Connection) => Unit

  def action: Action
  def action_=(value: Action): Unit
}