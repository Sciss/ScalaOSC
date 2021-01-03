/*
 * BrowserClientPlatform.scala
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

trait BrowserClientPlatform {
  def apply(target: Browser.Address): Browser.Client = apply(target, Config.default)

  def apply(target: Browser.Address, config: Config): Browser.Client =
    new impl.BrowserClientImpl(target, config)
}
