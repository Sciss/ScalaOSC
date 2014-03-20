/*
 * UDPClientImpl.scala
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
import java.net.SocketAddress

private[osc] final class UDPClientImpl(val channel: DatagramChannel,
                                       protected val target: SocketAddress,
                                       protected val config: UDP.Config)
  extends ClientImpl with UDPChannelImpl {

  protected val input   = UDP.Receiver   (channel, target, config)
  protected val output  = UDP.Transmitter(channel, target, config)
}
