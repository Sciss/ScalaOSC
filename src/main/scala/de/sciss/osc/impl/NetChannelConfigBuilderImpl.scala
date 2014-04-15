/*
 * NetChannelConfigBuilderImpl.scala
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

import java.net.{InetAddress, InetSocketAddress}

private[osc] trait NetChannelConfigBuilderImpl extends ChannelConfigBuilderImpl with Channel.Net.ConfigBuilder {
  private var localSocket = new InetSocketAddress("0.0.0.0", 0)

  final def localSocketAddress = localSocket
  final def localSocketAddress_=(addr: InetSocketAddress): Unit = localSocket = addr

  final def localPort_=(port: Int): Unit =
    localSocket = new InetSocketAddress(localSocket.getAddress, port)

  final def localAddress_=(address: InetAddress): Unit =
    localSocket = new InetSocketAddress(address, localSocket.getPort)

  final def localIsLoopback_=(loopback: Boolean): Unit =
    if (localSocket.getAddress.isLoopbackAddress != loopback) {
      localAddress = InetAddress.getByName(if (loopback) "127.0.0.1" else "0.0.0.0")
    }
}

