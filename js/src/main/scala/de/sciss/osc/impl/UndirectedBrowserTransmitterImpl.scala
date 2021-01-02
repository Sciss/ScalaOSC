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
import java.net.{InetSocketAddress, SocketAddress}

private[osc] final class UndirectedBrowserTransmitterImpl(protected val driver: BrowserDriver.Repr,
                                                          protected val config: Browser.Config)
  extends BrowserTransmitterImpl with Transmitter.Undirected.Net {

  override def isConnected: Boolean = isOpen

  override def connect(): Unit = ()

  override def send(p: Packet, target: SocketAddress): Unit = target match {
    case remote: InetSocketAddress =>
      val key = remote.getPort.toString
      val ep  = driver.getOrElse(key, throw new IOException(s"Target endpoint $key not found"))
      send(p, ep)

    case _ =>
      throw new UnsupportedOperationException(s"target $target is not an InetSocketAddress")
  }
}
