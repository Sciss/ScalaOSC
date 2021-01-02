/*
 * ChannelConfigBuilderImpl.scala
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

private[osc] trait ChannelConfigBuilderImpl extends Channel.ConfigBuilder {
  private var bufferSizeVar = 8192

  final def bufferSize: Int = bufferSizeVar

  final def bufferSize_=(size: Int): Unit = {
    if (size < 16) throw new IllegalArgumentException(s"Buffer size ($size) must be >= 16")
    bufferSizeVar = size
  }

  final var codec: PacketCodec = PacketCodec.default
}