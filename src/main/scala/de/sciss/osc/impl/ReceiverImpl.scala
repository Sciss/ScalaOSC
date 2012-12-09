package de.sciss.osc
package impl

import java.nio.channels.{ClosedChannelException, AsynchronousCloseException}
import java.io.IOException
import java.net.SocketAddress

private[osc] trait ReceiverImpl extends SingleInputChannelImpl with ThreadedImpl {
   rcv =>

   final protected def threadLoop() {
      try {
         receive()
      } catch {
         case _: AsynchronousCloseException  => closedException()
         case _: ClosedChannelException      => closedException()
      }
   }

   @throws( classOf[ IOException ])
   protected def receive() : Unit

   @throws( classOf[ IOException ])
   final def close() {
      stopThread()
      channel.close()
   }

   @throws( classOf[ IOException ])
   final def connect() {
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

private[osc] trait DirectedReceiverImpl extends ReceiverImpl with DirectedInputImpl {
   override def toString = transport.name + ".Receiver(" + target + ")@" + hashCode().toHexString

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
   final def isConnected = isOpen && isThreadRunning

   override def toString = transport.name + ".Receiver@" + hashCode().toHexString

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
