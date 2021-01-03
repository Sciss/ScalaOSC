/*
 * UndirectedBrowserTransmitterImpl.scala
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
package impl

import java.io.IOException

private[osc] final class UndirectedBrowserTransmitterImpl(protected val driver: BrowserDriver.Repr,
                                                          protected val config: Browser.Config)
  extends BrowserTransmitterImpl with Browser.Transmitter.Undirected {

  private[this] var _open = true

  override def isConnected: Boolean = _open

  override def connect(): Unit = ()

  override def isOpen: Boolean = _open

  override def close(): Unit =
    _open = false

  override def send(p: Packet, target: Browser.Address): Unit = {
    val key = target.port.toString
    val ep  = driver.getOrElse(key, throw new IOException(s"Target endpoint $key not found"))
    send(p, ep)
  }
}
