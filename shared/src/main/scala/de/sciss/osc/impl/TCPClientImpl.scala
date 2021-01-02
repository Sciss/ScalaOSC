/*
 * TCPClientImpl.scala
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

import java.net.SocketAddress
import java.nio.channels.SocketChannel

private[osc] final class TCPClientImpl(val channel: SocketChannel,
                                       protected val target: SocketAddress,
                                       protected val config: TCP.Config)
  extends ClientImpl with TCPSingleChannelImpl {

  protected val input : TCP.Receiver    = TCP.Receiver   (channel, target, config)
  protected val output: TCP.Transmitter = TCP.Transmitter(channel, target, config)
}
