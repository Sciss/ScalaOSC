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
import java.net.SocketAddress
import java.nio.{BufferUnderflowException, ByteBuffer}
import java.nio.channels.{AsynchronousCloseException, InterruptibleChannel, ClosedChannelException}

object OSCReceiver {
   trait Net extends OSCReceiver with OSCChannel.Net
}

trait OSCReceiver extends OSCChannel {
   rcv =>

  	var action                       = (msg: OSCMessage, sender: SocketAddress, time: Long ) => ()
	private val	threadSync           = new AnyRef
   @volatile private var wasClosed  = false
//   private val bufSync            = new AnyRef
   protected final val byteBuf	   = ByteBuffer.allocateDirect( config.bufferSize )

//	protected var	tgt : SocketAddress			= null

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
//   thread.start()

   @throws( classOf[ IOException ])
   protected def receive() : Unit

//	final def target: SocketAddress = tgt

   protected def channel: InterruptibleChannel

   /**
    *	Queries whether the <code>OSCReceiver</code> is
    *	listening or not.
    */
   final def isOpen : Boolean = channel.isOpen // generalSync.synchronized { !wasClosed }

//	/**
//	 *	Queries whether the <code>OSCReceiver</code> is
//	 *	listening or not.
//	 */
//	final def isOpen : Boolean = generalSync.synchronized { !wasClosed }
//
//   protected final def isOpenNoSync : Boolean = !wasClosed

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

//	/**
//	 *  Stops waiting for incoming messages. This
//	 *	method returns when the receiving thread has terminated.
//     *  To prevent deadlocks, this method cancels after
//     *  five seconds, calling <code>close()</code> on the datagram
//	 *	channel, which causes the listening thread to die because
//	 *	of a channel-closing exception.
//     *
//     *  @throws IOException if an error occurs while shutting down
//	 *
//	 *	@throws	IllegalStateException	when trying to call this method from within the OSC receiver thread
//	 *									(which would obviously cause a loop)
//	 */
//	@throws( classOf[ IOException ])
//	private def stop() {
//      threadSync.synchronized {
//         if( Thread.currentThread == thread ) throw new IllegalStateException( "Cannot be called from reception thread" )
//         if( !wasClosed ) {
//            if( thread.isAlive ) {
//               try {
//                  sendGuardSignal()
//                  threadSync.wait( 5000 )
//               }
//               catch { case e2: InterruptedException =>
//                  e2.printStackTrace()
//               }
//               finally {
//                  if( !wasClosed && thread.isAlive ) {
//                     try {
//                        Console.err.println( "OSCReceiver.stopListening : rude task killing (" + this.hashCode + ")" )
//                        closeChannel()
//                     }
//                     catch { case e3: IOException =>
//                        e3.printStackTrace()
//                     }
//                  }
//                  wasClosed = true
//               }
//            }
//         }
//		}
//	}

//	@throws( classOf[ IOException ])
//	protected def sendGuardSignal() : Unit
	
//	@throws( classOf[ IOException ])
//	protected def closeChannel() : Unit

	@throws( classOf[ IOException ])
	protected final def flipDecodeDispatch( sender: SocketAddress ) {
		try {
			byteBuf.flip()
			val p = codec.decode( byteBuf )
         dumpPacket( p )
			dispatchPacket( p, sender, OSCBundle.Now )	// OSCBundles will override this dummy time tag
		}
		catch { case e1: BufferUnderflowException =>
			if( !wasClosed ) {
				Console.err.println( new OSCException( OSCException.RECEIVE, e1.toString ))
			}
		}
	}

	private def dispatchPacket( p: OSCPacket, sender: SocketAddress, time: Long ) {
		if( p.isInstanceOf[ OSCMessage ]) {
			dispatchMessage( p.asInstanceOf[ OSCMessage ], sender, time )
		} else
//		if( p.isInstanceOf[ OSCBundle ])
		{
			val bndl	= p.asInstanceOf[ OSCBundle ]
			val time2	= bndl.timetag
			bndl.foreach( dispatchPacket( _, sender, time2 ))
//		} else {
//			assert false : p.getClass().getName();
		}
	}

	private def dispatchMessage( msg: OSCMessage, sender: SocketAddress, time: Long ) {
//		generalSync.synchronized {
//			if( action != null ) {
				action.apply( msg, sender, time )
//			}
//		}
	}

   /**
    * Callers should have a lock on the buffer!
    */
   private def dumpPacket( p: OSCPacket ) {
      if( (dumpMode ne OSCDump.Off) && dumpFilter( p )) {
         printStream.synchronized {
            printStream.print( "r: " )
            dumpMode match {
               case OSCDump.Text =>
                  OSCPacket.printTextOn( codec, printStream, p )
               case OSCDump.Hex =>
                  OSCPacket.printHexOn( printStream, byteBuf )
               case OSCDump.Both =>
                  OSCPacket.printTextOn( codec, printStream, p )
                  OSCPacket.printHexOn( printStream, byteBuf )
               case _ =>   // satisfy compiler
            }
         }
      }
   }
}
