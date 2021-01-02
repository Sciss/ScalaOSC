/*
 * DirectedUDPReceiverImpl.scala
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

import java.net.SocketAddress
import java.nio.Buffer
import java.nio.channels.DatagramChannel

private[osc] final class DirectedUDPReceiverImpl(val channel: DatagramChannel,
                                                 protected val target: SocketAddress,
                                                 protected val config: UDP.Config)
  extends DirectedReceiverImpl with SingleChannelDirectImpl with ThreadedReceiverImpl with UDPChannelImpl {

  def isConnected: Boolean = channel.isConnected && isThreadRunning

  protected def connectChannel(): Unit = if (!channel.isConnected) channel.connect(target)

  protected def receive(): Unit = {
    (buf: Buffer).clear()
    channel.receive(buf)
    flipDecodeDispatch()
  }
}