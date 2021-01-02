/*
 * BrowserTransmitterPlatform.scala
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

import de.sciss.osc.Browser.Config
import de.sciss.osc.impl.BrowserDriver

import java.net.{InetSocketAddress, SocketAddress}

trait BrowserTransmitterPlatform {
  def apply(): Browser.Transmitter.Undirected = apply(Config.default)

  def apply(config: Config): Browser.Transmitter.Undirected =
    new impl.UndirectedBrowserTransmitterImpl(BrowserDriver(), config)

  def apply(target: SocketAddress): Browser.Transmitter.Directed = apply(target, Config.default)

  def apply(target: SocketAddress, config: Config): Browser.Transmitter.Directed = target match {
    case remote: InetSocketAddress =>
      new impl.DirectedBrowserTransmitterImpl(BrowserDriver(), remote, config)
    case _ => throw new UnsupportedOperationException(s"target $target is not an InetSocketAddress")
  }
}
