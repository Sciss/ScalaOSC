/*
 * ThreadedImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2020 Hanns Holger Rutz. All rights reserved.
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
import java.nio.channels.ClosedChannelException

private[osc] trait ThreadedImpl {
  outer =>

  private[this] val threadSync: AnyRef = new AnyRef
  
  @volatile private[this] var wasClosed = false

  final protected def closedException(): Unit =
    threadSync.synchronized {
    if (!wasClosed) {
      Console.err.println(s"$outer : Connection closed by remote side.")
      wasClosed = true
    }
  }

  protected def threadLoop(): Unit

  private[this] val thread: Thread = new Thread(outer.toString) {
    override def run(): Unit =
      try {
        while (!wasClosed) threadLoop()
      } finally {
        threadSync.synchronized {
          wasClosed = true
          threadSync.notifyAll()
        }
      }
  }

  @throws(classOf[IOException])
  final protected def startThread(): Unit =
    try {
      thread.start()
    } catch {
      case _: IllegalThreadStateException => throw new ClosedChannelException()
    }

   final protected def stopThread(): Unit = 
     threadSync.synchronized {
      wasClosed = true
    }

  final protected def isThreadRunning: Boolean =
    thread.isAlive && {
      val _wasClosed = threadSync.synchronized { wasClosed }
      !_wasClosed
    }
}