/*
 * UDPConfigImpl.scala
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

import java.net.{InetAddress, InetSocketAddress}
import java.nio.channels.DatagramChannel

private[osc] final class UDPConfigBuilderImpl extends NetChannelConfigBuilderImpl with UDP.ConfigBuilder {
  def transport: UDP.type = UDP

  def build: UDP.Config = UDPConfigImpl(bufferSize, codec, localSocketAddress)
}

private[osc] final case class UDPConfigImpl(bufferSize: Int, codec: PacketCodec,
                                            localSocketAddress: InetSocketAddress)
  extends UDP.Config {

  def transport: UDP.type = UDP

  def openChannel(discardWildcard: Boolean): DatagramChannel = {
    val ch    = DatagramChannel.open()
    val addr0 = localSocketAddress
    val addr  = if (discardWildcard && addr0.getAddress == InetAddress.getByAddress(new Array[Byte](4))) {
      new InetSocketAddress(InetAddress.getLocalHost, addr0.getPort)
    } else addr0

    ch.socket().bind(addr)
    ch
  }
}
