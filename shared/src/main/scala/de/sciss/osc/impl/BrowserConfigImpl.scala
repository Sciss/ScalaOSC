/*
 * BrowserConfigImpl.scala
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

private[osc] final class BrowserConfigBuilderImpl extends NetChannelConfigBuilderImpl with Browser.ConfigBuilder {
  def transport: Browser.type = Browser

  def build: Browser.Config =
    BrowserConfigImpl(bufferSize = bufferSize, codec = codec, localSocketAddress = localSocketAddress)
}

private[osc] final case class BrowserConfigImpl(bufferSize: Int, codec: PacketCodec,
                                                localSocketAddress: InetSocketAddress)
  extends Browser.Config {

  def transport: Browser.type = Browser
}
