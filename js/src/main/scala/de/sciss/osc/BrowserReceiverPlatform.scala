/*
 * BrowserReceiverPlatform.scala
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

trait BrowserReceiverPlatform {
  def apply(): Browser.Receiver.Undirected = apply(Config.default)

  def apply(config: Config): Browser.Receiver.Undirected =
    new impl.UndirectedBrowserReceiverImpl(BrowserDriver(), config)

  def apply(target: Browser.Address): Browser.Receiver.Directed = apply(target, Config.default)

  def apply(target: Browser.Address, config: Config): Browser.Receiver.Directed =
    new impl.DirectedBrowserReceiverImpl(BrowserDriver(), target, config)
}