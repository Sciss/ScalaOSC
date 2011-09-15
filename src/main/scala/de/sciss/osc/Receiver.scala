/*
 * OSCReceiver.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2011 Hanns Holger Rutz. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc

import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.channels.{AsynchronousCloseException, ClosedChannelException}
import java.net.SocketAddress

object Receiver {
   trait Net extends Receiver with Channel.NetConfigLike // Channel.Net

   object Directed {
      type Action = Packet => Unit
      val NoAction : Action = _ => ()
   }
   trait Directed extends Receiver {
      var action = Directed.NoAction

      @throws( classOf[ IOException ])
      protected final def flipDecodeDispatch() {
         try {
            buf.flip()
            val p = codec.decode( buf )
            try {
               action.apply( p )
            } catch {
               case e => e.printStackTrace() // XXX eventually error handler?
            }
         }
         catch { case e1: BufferUnderflowException =>
            if( !wasClosed ) {
               Console.err.println( new OSCException( OSCException.RECEIVE, e1.toString ))
            }
         }
      }
   }

   type DirectedNet = Directed with Net

   object Undirected {
      type Action = (Packet, SocketAddress) => Unit
      val NoAction : Action = (_, _) => ()
   }
   trait UndirectedNet extends Receiver {
      var action = Undirected.NoAction

      protected final def connectChannel() {}  // XXX or: if( !isOpen ) throw new ChannelClosedException ?

      /**
       * @param   sender   the remote socket from which the packet was sent.
       *                   this may be `null` in which case this method does nothing.
       */
      @throws( classOf[ IOException ])
      protected final def flipDecodeDispatch( sender: SocketAddress ) {
         if( sender != null ) try {
            buf.flip()
            val p = codec.decode( buf )
            try {
               action.apply( p, sender )
            } catch {
               case e => e.printStackTrace() // XXX eventually error handler?
            }
         }
         catch { case e1: BufferUnderflowException =>
            if( !wasClosed ) {
               Console.err.println( new OSCException( OSCException.RECEIVE, e1.toString ))
            }
         }
      }
   }
}

trait Receiver extends Channel.Input {
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

      override def run {
         try {
            while( !wasClosed ) receive()
         } catch {
            case e: AsynchronousCloseException => closedException()
            case e: ClosedChannelException => closedException()
         } finally {
            threadSync.synchronized {
               wasClosed = true
               threadSync.notifyAll()
            }
         }
      }
   }
	thread.setDaemon( true )

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
   protected def connectChannel() : Unit

   @throws( classOf[ IOException ])
   final def connect() {
      connectChannel()
      start()
   }

   @throws( classOf[ IOException ])
   private def start() {
      try {
         thread.start()
      } catch {
         case _: IllegalThreadStateException => throw new ClosedChannelException()
      }
   }
}