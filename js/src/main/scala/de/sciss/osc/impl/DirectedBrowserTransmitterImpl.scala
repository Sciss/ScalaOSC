/*
 * DirectedBrowserTransmitterImpl.scala
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
import java.net.InetSocketAddress

private[osc] final class DirectedBrowserTransmitterImpl(protected val driver: BrowserDriver.Repr,
                                                        val remoteSocketAddress: InetSocketAddress,
                                                        protected val config: Browser.Config)
  extends BrowserTransmitterImpl with Channel.Directed.Output with Channel.Directed.Net {

  override def !(p: Packet): Unit = bufSync.synchronized {
    if (ep == null) throw new IOException("Channel not yet connected")
    send(p, ep)
  }

  private[this] var ep: BrowserEndpoint = null

  def connect(): Unit = bufSync.synchronized {
    if (!isConnected) {
      val key = remotePort.toString
      ep      = driver.getOrElse(key, throw new IOException(s"Target endpoint $key not found"))
    }
  }

  override def isConnected: Boolean = bufSync.synchronized { ep != null }
}