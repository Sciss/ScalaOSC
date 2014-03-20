/*
 * UndirectedUDPTransmitterImpl.scala
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

import java.nio.channels.DatagramChannel
import de.sciss.osc.Transmitter
import java.io.IOException
import java.net.SocketAddress

private[osc] final class UndirectedUDPTransmitterImpl(val channel: DatagramChannel,
                                                      protected val config: UDP.Config)
  extends UDPTransmitterImpl with Transmitter.Undirected.Net {

  override def toString = s"${transport.name}.Transmitter@${hashCode().toHexString}"

  @throws(classOf[IOException])
  def send(p: Packet, target: SocketAddress): Unit = bufSync.synchronized {
    buf.clear()
    p.encode(codec, buf)
    buf.flip()
    dumpPacket(p)
    channel.send(buf, target)
  }

  @throws(classOf[IOException])
  protected def connectChannel() = ()

  // XXX or: if( !isOpen ) throw new ChannelClosedException ?
  def isConnected = isOpen
}
