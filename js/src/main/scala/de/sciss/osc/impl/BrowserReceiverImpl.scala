/*
 * BrowserReceiverImpl.scala
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

import java.io.IOException
import java.nio.{Buffer, BufferOverflowException}
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.util.control.NonFatal

private[osc] abstract class BrowserReceiverImpl(driver: BrowserDriver.Repr,
                                                protected val config: Browser.Config)
  extends SingleInputChannelImpl
    with BrowserChannelWrapImpl {

  private[this] var _open       = true
  private[this] var _connected  = false

  override def connect(): Unit = bufSync.synchronized {
    if (!isConnected) {
      val key = localAddress.port.toString
      if (driver.contains(key)) throw new IOException(s"Channel $localAddress already bound")

      driver.put(key, ep)
      _connected = true
    }
  }

  protected def filterPort(remotePort: Int): Boolean

  protected def dispatch(p: Packet, remotePort: Int): Unit

  private object ep extends BrowserEndpoint {
    val receive: js.Function2[Int, Uint8Array, Unit] = { (remotePort, data) =>
      try {
        if (filterPort(remotePort)) {
          (buf: Buffer).clear()
          val sz = data.length
          if (sz > arrayBuf.byteLength) throw new BufferOverflowException
          val uInt8Buf = new Uint8Array(arrayBuf, 0, sz)
          uInt8Buf.set(data)
          (buf: Buffer).position(sz)

          (buf: Buffer).flip()
          val p = codec.decode(buf)
          dumpPacket(p)
          try {
            dispatch(p, remotePort)
          } catch {
            case NonFatal(e) => e.printStackTrace() // XXX eventually error handler?
          }
        }
      } catch {
        case NonFatal(ex) =>
          ex.printStackTrace()
      }
    }
  }

  override def isConnected: Boolean = _connected

  override def isOpen: Boolean = _open

  override def close(): Unit = bufSync.synchronized {
    _open = false
    if (_connected) {
      val key = localAddress.port.toString
      driver.remove(key)
      _connected = false
    }
  }
}
