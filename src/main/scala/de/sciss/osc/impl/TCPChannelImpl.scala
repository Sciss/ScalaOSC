/*
 * TCPChannelImpl.scala
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

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

private[osc] trait TCPChannelImpl extends ChannelImpl {
  protected def config: TCP.Config

  final def transport: Transport.Net = config.transport
}

private[osc] trait TCPSingleChannelImpl extends TCPChannelImpl with TCP.Channel with DirectedImpl {
  override def channel: SocketChannel

  override protected def config: TCP.Config

  final def localSocketAddress: InetSocketAddress = {
    val so = channel.socket()
    new InetSocketAddress(so.getLocalAddress, so.getLocalPort)
  }

  final protected def connectChannel(): Unit =
    if (!channel.isConnected) channel.connect(target)

  final def remoteSocketAddress: InetSocketAddress = {
    val so = channel.socket()
    new InetSocketAddress(so.getInetAddress, so.getPort)
  }
}
