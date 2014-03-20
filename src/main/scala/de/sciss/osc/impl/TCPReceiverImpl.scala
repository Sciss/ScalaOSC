/*
 * TCPReceiverImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2018-2014 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc
package impl

import java.nio.channels.SocketChannel
import java.net.SocketAddress

private[osc] final class TCPReceiverImpl(val channel: SocketChannel,
                                         protected val target: SocketAddress,
                                         protected val config: TCP.Config)
  extends DirectedReceiverImpl with TCPSingleChannelImpl {

  def isConnected = channel.isConnected && isThreadRunning

  protected def receive(): Unit = {
    buf.rewind().limit(4) // in TCP mode, first four bytes are packet size in bytes
    do {
      val len = channel.read(buf)
      if (len == -1) return
    } while (buf.hasRemaining)

    buf.rewind()
    val packetSize = buf.getInt()
    buf.rewind().limit(packetSize)

    while (buf.hasRemaining) {
      val len = channel.read(buf)
      if (len == -1) return
    }

    flipDecodeDispatch()
  }
}