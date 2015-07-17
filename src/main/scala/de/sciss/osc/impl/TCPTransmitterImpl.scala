/*
 * TCPTransmitterImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2015 Hanns Holger Rutz. All rights reserved.
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
import java.io.IOException

private[osc] final class TCPTransmitterImpl(val channel: SocketChannel,
                                            protected val target: SocketAddress,
                                            protected val config: TCP.Config)
  extends TransmitterImpl with TCPSingleChannelImpl with Channel.Directed.Output {

  override def toString: String = s"${TCP.name}.Transmitter($target)@${hashCode().toHexString}"

  def isConnected: Boolean = channel.isConnected

  @throws(classOf[IOException])
  def ! (p: Packet): Unit = bufSync.synchronized {
    buf.clear()
    buf.position(4)
    p.encode(codec, buf)
    val len = buf.position() - 4
    buf.flip()
    buf.putInt(0, len)
    dumpPacket(p)
    channel.write(buf)
  }
}
