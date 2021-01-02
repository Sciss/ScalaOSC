/*
 * UDPChannelImpl.scala
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

import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

private[osc] trait UDPChannelImpl extends ChannelImpl with UDP.Channel {
  override protected def config: UDP.Config

  final def transport: Transport.Net = config.transport

  override def channel: DatagramChannel

  final def localSocketAddress: InetSocketAddress = {
    val so = channel.socket()
    new InetSocketAddress(so.getLocalAddress, so.getLocalPort)
  }

  final def remoteSocketAddress: InetSocketAddress = {
    val so = channel.socket()
    new InetSocketAddress(so.getInetAddress, so.getPort)
  }
}
