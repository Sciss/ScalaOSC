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

// no constructors on the JVM
trait BrowserReceiverPlatform {
  def apply(target: Int, config: Config): Browser.Receiver.Directed =
    throw new UnsupportedOperationException("Browser.Receiver not supported on the JVM")
}