/*
 * ReceiverImpl.scala
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
import java.net.SocketAddress
import java.nio.Buffer
import java.nio.channels.{AsynchronousCloseException, ClosedChannelException}

import scala.util.control.NonFatal

private[osc] trait ThreadedReceiverImpl extends SingleInputChannelImpl with ThreadedImpl {
  rcv =>

  /** Requests to connect the network channel. This may be called several
    * times, and the implementation should ignore the call when the channel
    * is already connected.
    */
  @throws(classOf[IOException])
  protected def connectChannel(): Unit

  final protected def threadLoop(): Unit =
    try {
      receive()
    } catch {
      case _: AsynchronousCloseException => closedException()
      case _: ClosedChannelException     => closedException()
    }

  @throws(classOf[IOException])
  protected def receive(): Unit

  @throws(classOf[IOException])
  final def close(): Unit = {
    stopThread()
    channel.close()
  }

  @throws(classOf[IOException])
  final def connect(): Unit = {
    connectChannel()
    startThread()
  }

   /*
      Idea:
      start with channel unconnected
      after first packet with correct address,
      do the connect
    */
}

private[osc] trait DirectedReceiverImpl extends SingleInputChannelImpl /*ReceiverImpl*/ with DirectedInputImpl {
  override def toString: String = s"${transport.name}.Receiver($target)@${hashCode().toHexString}"

  @throws(classOf[PacketCodec.Exception])
  final protected def flipDecodeDispatch(): Unit = {
    (buf: Buffer).flip()
    val p = codec.decode(buf)
    dumpPacket(p)
    try {
      action.apply(p)
    } catch {
      case NonFatal(e) => e.printStackTrace() // XXX eventually error handler?
    }
  }
}

private[osc] trait UndirectedNetReceiverImpl extends SingleInputChannelImpl /*ReceiverImpl*/ with UndirectedNetInputImpl {

  @throws(classOf[IOException])
  protected final def connectChannel(): Unit = ()

  // XXX or: if( !isOpen ) throw new ChannelClosedException ?
  // final def isConnected: Boolean = isOpen && isThreadRunning

  override def toString: String = s"${transport.name}.Receiver@${hashCode().toHexString}"

  /**
    * @param   sender   the remote socket from which the packet was sent.
    *                   this may be `null` in which case this method does nothing.
    */
  @throws(classOf[Exception])
  protected final def flipDecodeDispatch(sender: SocketAddress): Unit =
    if (sender != null) /* try */ {
      (buf: Buffer).flip()
      val p = codec.decode(buf)
      dumpPacket(p)
      try {
        action.apply(p, sender)
      } catch {
        case NonFatal(e) => e.printStackTrace() // XXX eventually error handler?
      }
    }
}
