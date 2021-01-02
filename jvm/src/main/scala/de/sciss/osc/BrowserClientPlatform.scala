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

import java.net.SocketAddress

// no constructors on the JVM
trait BrowserClientPlatform {
  def apply(target: SocketAddress, config: Config): Browser.Client =
    throw new UnsupportedOperationException("Browser.Receiver not supported on the JVM")
}