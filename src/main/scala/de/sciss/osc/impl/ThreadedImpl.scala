package de.sciss.osc
package impl

import java.io.IOException
import java.nio.channels.ClosedChannelException

private[osc] trait ThreadedImpl {
   outer =>

   private val	threadSync           = new AnyRef
     @volatile private var wasClosed  = false

     final protected def closedException() {
        threadSync.synchronized {
           if( !wasClosed ) {
              Console.err.println( outer.toString + " : Connection closed by remote side." )
              wasClosed = true
           }
        }
     }

   protected def threadLoop() : Unit

  	private val thread = new Thread( outer.toString ) {
//        setDaemon( true )

        override def run() {
           try {
              while( !wasClosed ) threadLoop()
           } finally {
              threadSync.synchronized {
                 wasClosed = true
                 threadSync.notifyAll()
              }
           }
        }
     }

   @throws( classOf[ IOException ])
   final protected def startThread() {
      try {
         thread.start()
      } catch {
         case _: IllegalThreadStateException => throw new ClosedChannelException()
      }
   }

   final protected def stopThread() {
      threadSync.synchronized {
         wasClosed = true
      }
   }

   final protected def isThreadRunning : Boolean = {
      thread.isAlive && threadSync.synchronized( !wasClosed )
   }
}