/*
 * BrowserChannelImpl.scala
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
import java.nio.ByteBuffer
import java.nio.channels.InterruptibleChannel
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

private[osc] trait BrowserChannelWrapImpl extends BrowserChannelImpl {
  final protected val arrayBuf = new ArrayBuffer(config.bufferSize)

  protected final val buf: ByteBuffer = TypedArrayBuffer.wrap(arrayBuf)
}

private[osc] trait BrowserChannelImpl extends ChannelImpl with Browser.Channel {

  override protected def config: Browser.Config

  final def transport: Transport.Net = config.transport

  override object channel extends InterruptibleChannel {
    private[this] var _open = true

    override def close(): Unit =
      _open = false

    override def isOpen: Boolean = _open
  }

  override def localSocketAddress: InetSocketAddress = config.localSocketAddress
}
