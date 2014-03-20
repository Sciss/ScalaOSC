/*
 * UDPChannelImpl.scala
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
import java.net.InetSocketAddress

private[osc] trait UDPChannelImpl extends ChannelImpl with UDP.Channel {
   override protected def config: UDP.Config
   final def transport = config.transport
   override def channel: DatagramChannel

   final def localSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
   }

   final def remoteSocketAddress = {
      val so = channel.socket()
      new InetSocketAddress( so.getInetAddress, so.getPort )
   }
}
