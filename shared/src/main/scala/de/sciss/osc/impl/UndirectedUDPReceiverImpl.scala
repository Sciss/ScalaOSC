/*
 * UndirectedUDPReceiverImpl.scala
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

import java.nio.Buffer
import java.nio.channels.DatagramChannel

private[osc] final class UndirectedUDPReceiverImpl(val channel: DatagramChannel,
                                                   protected val config: UDP.Config)
  extends UndirectedNetReceiverImpl with SingleChannelDirectImpl with ThreadedReceiverImpl with UDPChannelImpl with Channel.Undirected.Input.Net {

  override def isConnected: Boolean = isOpen && isThreadRunning

  protected def receive(): Unit = {
    (buf: Buffer).clear()
    val sender = channel.receive(buf)
    flipDecodeDispatch(sender)
  }
}
