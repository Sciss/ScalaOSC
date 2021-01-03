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

import de.sciss.osc.Channel.Directed.Input.{Action, NoAction}

private[osc] final class DirectedBrowserReceiverImpl(driver: BrowserDriver.Repr,
                                                     remoteSocketAddress: Browser.Address,
                                                     config: Browser.Config)
  extends BrowserReceiverImpl(driver, config)
    with Channel.Directed.Input with Browser.ConfigLike with Browser.Receiver.Directed {

  var action: Action = NoAction

  override protected def filterPort(remotePort: Int): Boolean =
    remotePort == remoteSocketAddress.port

  override protected def dispatch(p: Packet, remotePort: Int): Unit =
    action(p)
}
