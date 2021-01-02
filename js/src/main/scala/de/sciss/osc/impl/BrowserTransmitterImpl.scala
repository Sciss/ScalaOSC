/*
 * BrowserTransmitterImpl.scala
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

import java.nio.Buffer
import scala.scalajs.js.typedarray.Uint8Array

private[osc] trait BrowserTransmitterImpl extends TransmitterImpl with BrowserChannelWrapImpl {
  protected def driver: BrowserDriver.Repr

  override def toString: String = s"${transport.name}.Transmitter@${hashCode().toHexString}"

  // caller must synchronize on `bufSync`!
  final protected def send(p: Packet, ep: BrowserEndpoint): Unit = {
    (buf: Buffer).clear()
    p.encode(codec, buf)
    (buf: Buffer).flip()
    val sz = buf.limit()
    dumpPacket(p)
    val uInt8Buf = new Uint8Array(arrayBuf, 0, sz)
    ep.receive(localPort, uInt8Buf)
  }
}