/*
 * UDPClientImpl.scala
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
import java.nio.channels.DatagramChannel

private[osc] final class UDPClientImpl(val channel: DatagramChannel,
                                       protected val target: SocketAddress,
                                       protected val config: UDP.Config)
  extends ClientImpl[SocketAddress] with UDPChannelImpl {

  protected val input : UDP.Receiver   .Directed = UDP.Receiver   (channel, target, config)
  protected val output: UDP.Transmitter.Directed = UDP.Transmitter(channel, target, config)
}
