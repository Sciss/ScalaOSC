/*
 * DirectedUDPReceiverImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2014 Hanns Holger Rutz. All rights reserved.
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
import java.net.SocketAddress

private[osc] final class DirectedUDPReceiverImpl(val channel: DatagramChannel,
                                                 protected val target: SocketAddress,
                                                 protected val config: UDP.Config)
  extends DirectedReceiverImpl with UDPChannelImpl {

  def isConnected = channel.isConnected && isThreadRunning

  protected def connectChannel(): Unit = if (!channel.isConnected) channel.connect(target)

  protected def receive(): Unit = {
    buf.clear()
    channel.receive(buf)
    flipDecodeDispatch()
  }
}