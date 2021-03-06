/*
 * UndirectedUDPTransmitterImpl.scala
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

private[osc] final class UndirectedUDPTransmitterImpl(val channel: DatagramChannel,
                                                      protected val config: UDP.Config)
  extends UDPTransmitterImpl with Transmitter.Undirected.Net {

  override def toString: String = s"${transport.name}.Transmitter@${hashCode().toHexString}"

  @throws(classOf[IOException])
  def send(p: Packet, target: SocketAddress): Unit = bufSync.synchronized {
    (buf: Buffer).clear()
    p.encode(codec, buf)
    (buf: Buffer).flip()
    dumpPacket(p)
    channel.send(buf, target)
  }

  @throws(classOf[IOException])
  def connect(): Unit = ()

  // XXX or: if( !isOpen ) throw new ChannelClosedException ?
  def isConnected: Boolean = isOpen
}
