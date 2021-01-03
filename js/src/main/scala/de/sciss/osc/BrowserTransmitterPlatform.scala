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

trait BrowserTransmitterPlatform {
  def apply(): Browser.Transmitter.Undirected = apply(Config.default)

  def apply(config: Config): Browser.Transmitter.Undirected =
    new impl.UndirectedBrowserTransmitterImpl(BrowserDriver(), config)

  def apply(target: Browser.Address): Browser.Transmitter.Directed = apply(target, Config.default)

  def apply(target: Browser.Address, config: Config): Browser.Transmitter.Directed =
      new impl.DirectedBrowserTransmitterImpl(BrowserDriver(), target, config)
}
