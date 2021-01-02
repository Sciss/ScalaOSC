/*
 * DirectedUDPTransmitterImpl.scala
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
import java.net.SocketAddress
import java.nio.Buffer
import java.nio.channels.DatagramChannel

private[osc] final class DirectedUDPTransmitterImpl(val channel: DatagramChannel,
                                                    protected val target: SocketAddress,
                                                    protected val config: UDP.Config)
  extends UDPTransmitterImpl with Channel.Directed.Output with Channel.Directed.Net {

  override def toString: String = s"${transport.name}.Transmitter@${hashCode().toHexString}"

  @throws(classOf[IOException])
  def connect(): Unit =
    if (!isConnected) channel.connect(target)

  def isConnected: Boolean = channel.isConnected

  @throws(classOf[IOException])
  def ! (p: Packet): Unit = bufSync.synchronized {
    (buf: Buffer).clear()
    p.encode(codec, buf)
    (buf: Buffer).flip()
    dumpPacket(p)
    channel.write(buf)
  }
}
