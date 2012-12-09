package de.sciss.osc
package impl

import java.nio.channels.{ClosedChannelException, AsynchronousCloseException}
import java.io.IOException
import java.net.SocketAddress

private[osc] trait ReceiverImpl extends SingleInputChannelImpl {
   rcv =>

	private val	threadSync           = new AnyRef
   @volatile private var wasClosed  = false

	private val thread = new Thread( this.toString ) {
      private def closedException() {
         threadSync.synchronized {
            if( !wasClosed ) {
               Console.err.println( rcv.toString + " : Connection closed by remote side." )
               wasClosed = true
            }
         }
      }

      override def run() {
//println( "RECEIVER RUN " + this )
         try {
            while( !wasClosed ) {
               try {
                  receive()
               } catch {
                  case e: AsynchronousCloseException => closedException()
                  case e: ClosedChannelException => closedException()
//                  case e: PortUnreachableException =>
//                   // thrown by server coming up (e.g. scsynth booting)
//                     Thread.sleep( 50 )
               }
            }
         } finally {
//println( "RECEIVER EXIT " + this )
            threadSync.synchronized {
               wasClosed = true
               threadSync.notifyAll()
            }
         }
      }
   }
//	thread.setDaemon( true )

   @throws( classOf[ IOException ])
   protected def receive() : Unit

   @throws( classOf[ IOException ])
   final def close() {
      threadSync.synchronized {
         wasClosed = true
         channel.close()
      }
   }

   @throws( classOf[ IOException ])
   final def connect() {
//println( "RCV: connectChannel()" )
      connectChannel()
//println( "RCV: start()" )
      start()
//println( "RCV: started" )
   }

//   final def isConnected : Boolean = isChannelConnected && thread.isAlive

//   final def isConnected : Boolean = {
//      // XXX
//      check if thread is sleeping due to port unreachable!
//
//   }

   /*
      Idea:
      start with channel unconnected
      after first packet with correct address,
      do the connect
    */

   @throws( classOf[ IOException ])
   private def start() {
      try {
         thread.start()
      } catch {
         case _: IllegalThreadStateException => throw new ClosedChannelException()
      }
   }
}

private[osc] trait DirectedReceiverImpl extends ReceiverImpl with DirectedInputImpl {
   override def toString = transport.name + ".Receiver(" + target + ")"

   @throws( classOf[ PacketCodec.Exception ])
   final protected def flipDecodeDispatch() {
      buf.flip()
      val p = codec.decode( buf )
      dumpPacket( p )
      try {
         action.apply( p )
      } catch {
         case e: Throwable => e.printStackTrace() // XXX eventually error handler?
      }
   }
}

private[osc] trait UndirectedNetReceiverImpl extends ReceiverImpl with UndirectedNetInputImpl {
   @throws( classOf[ IOException ])
   protected final def connectChannel() {}  // XXX or: if( !isOpen ) throw new ChannelClosedException ?
   final def isConnected = isOpen

   override def toString = transport.name + ".Receiver()"

   /**
    * @param   sender   the remote socket from which the packet was sent.
    *                   this may be `null` in which case this method does nothing.
    */
   @throws( classOf[ Exception ])
   protected final def flipDecodeDispatch( sender: SocketAddress ) {
      if( sender != null ) /* try */ {
         buf.flip()
         val p = codec.decode( buf )
         dumpPacket( p )
         try {
            action.apply( p, sender )
         } catch {
            case e: Throwable => e.printStackTrace() // XXX eventually error handler?
         }
      }
   }
}
