/*
 * UndirectedUDPReceiverImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2018 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc
package impl

import java.nio.channels.DatagramChannel

private[osc] final class UndirectedUDPReceiverImpl(val channel: DatagramChannel,
                                                   protected val config: UDP.Config)
  extends UndirectedNetReceiverImpl with UDPChannelImpl with Channel.Undirected.Input.Net {

  protected def receive(): Unit = {
    buf.clear()
    val sender = channel.receive(buf)
    flipDecodeDispatch(sender)
  }
}
