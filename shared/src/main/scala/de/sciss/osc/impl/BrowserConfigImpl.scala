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

private[osc] final class BrowserConfigBuilderImpl extends ChannelConfigBuilderImpl with Browser.ConfigBuilder {
  def transport: Browser.type = Browser

  override var localAddress: Browser.Address = Browser.Address(0)

  def build: Browser.Config =
    BrowserConfigImpl(bufferSize = bufferSize, codec = codec, localAddress = localAddress)
}

private[osc] final case class BrowserConfigImpl(bufferSize: Int, codec: PacketCodec,
                                                localAddress: Browser.Address)
  extends Browser.Config {

  def transport: Browser.type = Browser
}
