/*
 * UndirectedBrowserReceiverImpl.scala
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

import java.net.InetSocketAddress

private[osc] final class UndirectedBrowserReceiverImpl(driver: BrowserDriver.Repr, config: Browser.Config)
  extends BrowserReceiverImpl(driver, config)
    with UndirectedNetReceiverImpl {

  override protected def filterPort(remotePort: Int): Boolean = true

  override protected def dispatch(p: Packet, remotePort: Int): Unit = {
    val target = InetSocketAddress.createUnresolved("127.0.0.1", remotePort)
    action(p, target)
  }
}
