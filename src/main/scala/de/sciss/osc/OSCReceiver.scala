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

import impl.{TCPReceiver, UDPReceiver}
import java.io.{ IOException, PrintStream }
import java.net.{ DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress, Socket, SocketAddress,
                  UnknownHostException }
import java.nio.{ BufferUnderflowException, ByteBuffer }
import java.nio.channels.{ AlreadyConnectedException, ClosedChannelException, DatagramChannel, SelectableChannel,
	 							   SocketChannel }
import OSCChannel._
import ScalaOSC._

/**
 *    @version 0.11, 27-May-10
 */
object OSCReceiver {
	/**
	 *	Creates a new instance of a revivable <code>OSCReceiver</code>, using
	 *	default codec and a specific transport protocol and port. It
	 *	uses the local machine's IP or the &quot;loopback&quot; address.
	 *	Note that the <code>port</code> specifies the
	 *	local socket (at which the receiver listens), it does not determine the
	 *	remote sockets from which messages can be received. If you want to filter
	 *	out a particular remote (or target) socket, this can be done
	 *	using the <code>setTarget</code> method!
	 *	<P>
	 *	<B>TCP</B> receivers are required
	 *	to be connected to one particular target, so <code>setTarget</code> is
	 *	must be called prior to <code>connect</code> or <code>startListening</code>! 
	 *
	 *	@param	transport	the protocol to use, currently either <code>UDP</code> or <code>TCP</code>
	 *	@param	port		the port number for the OSC socket, or <code>0</code> to use an arbitrary free port
	 *	@param	loopBack	if <code>true</code>, the &quot;loopback&quot; address (<code>&quot;127.0.0.0.1&quot;</code>)
	 *						is used which limits communication to the local machine. If <code>false</code>, the
	 *						local machine's regular IP address is used.
	 *	
	 *	@return				the newly created receiver
	 *
	 *	@throws	IOException					if a networking error occurs while creating the socket
	 *	@throws	IllegalArgumentException	if an illegal protocol is used
	 */
//	@throws( classOf[ IOException ])
//	def apply( transport: OSCTransport, port: Int = 0, loopBack: Boolean = false,
//              codec: OSCPacketCodec = OSCPacketCodec.default ) : OSCReceiver = {
//		val localAddress = new InetSocketAddress( if( loopBack ) "127.0.0.1" else "0.0.0.0", port )
//		withAddress( transport, localAddress, codec )
//	}

	/**
	 *	Creates a new instance of a revivable <code>OSCReceiver</code>, using
	 *	default codec and a specific transport protocol and local socket address.
	 *	Note that <code>localAdress</code> specifies the
	 *	local socket (at which the receiver listens), it does not determine the
	 *	remote sockets from which messages can be received. If you want to filter
	 *	out a particular remote (or target) socket, this can be done
	 *	using the <code>setTarget</code> method!
	 *	<P>
	 *	<B>TCP</B> receivers are required
	 *	to be connected to one particular target, so <code>setTarget</code> is
	 *	must be called prior to <code>connect</code> or <code>startListening</code>! 
	 *
	 *	@param	transport		the protocol to use, currently either <code>UDP</code> or <code>TCP</code>
	 *	@param	localAddress	a valid address to use for the OSC socket. If the port is <code>0</code>,
	 *							an arbitrary free port is picked when the receiver is started. (you can find out
	 *							the actual port in this case by calling <code>getLocalAddress()</code> after the
	 *							receiver was started).
	 *	
	 *	@return					the newly created receiver
	 *
	 *	@throws	IOException					if a networking error occurs while creating the socket
	 *	@throws	IllegalArgumentException	if an illegal protocol is used
	 */
//	@throws( classOf[ IOException ])
//	def withAddress( transport: OSCTransport, localAddress: InetSocketAddress,
//                    codec: OSCPacketCodec = OSCPacketCodec.default ) : OSCReceiver = {
//      transport match {
//         case UDP => new UDPReceiver( localAddress, codec )
//         case TCP => new TCPReceiver( localAddress, codec )
//      }
//	}

	/**
	 *	Creates a new instance of a non-revivable <code>OSCReceiver</code>, using
	 *	default codec and UDP transport on a given channel. The caller should ensure that
	 *	the provided channel's socket was bound to a valid address
	 *	(using <code>dch.socket().bind( SocketAddress )</code>).
	 *	Note that <code>dch</code> specifies the
	 *	local socket (at which the receiver listens), it does not determine the
	 *	remote sockets from which messages can be received. If you want to filter
	 *	out a particular remote (or target) socket, this can be done
	 *	using the <code>setTarget</code> method!
	 *
	 *	@param	dch			the <code>DatagramChannel</code> to use as UDP socket.
	 *	@return				the newly created receiver
	 *
	 *	@throws	IOException	if a networking error occurs while configuring the socket
	 */
//	@throws( classOf[ IOException ])
//	def withChannel( dch: DatagramChannel ) : OSCReceiver =
//		new UDPReceiver( dch, OSCPacketCodec.default )

//   @throws( classOf[ IOException ])
//   def withChannel( dch: DatagramChannel, codec: OSCPacketCodec ) : OSCReceiver =
//      new UDPReceiver( dch, codec )

	/**
	 *	Creates a new instance of a non-revivable <code>OSCReceiver</code>, using
	 *	default codec and TCP transport on a given channel. The caller should ensure that
	 *	the provided channel's socket was bound to a valid address
	 *	(using <code>sch.socket().bind( SocketAddress )</code>). Furthermore,
	 *	the channel must be connected (using <code>connect()</code>) before
	 *	being able to receive messages. Note that <code>sch</code> specifies the
	 *	local socket (at which the receiver listens), it does not determine the
	 *	remote sockets from which messages can be received. The remote (or target)
	 *	socket must be explicitly specified using <code>setTarget</code> before
	 *	trying to connect!
	 *
	 *	@param	sch			the <code>SocketChannel</code> to use as TCP socket.
	 *	@return				the newly created receiver
	 *
	 *	@throws	IOException	if a networking error occurs while configuring the socket
	 */
//	@throws( classOf[ IOException ])
//	def withChannel( sch: SocketChannel, codec: OSCPacketCodec ) : OSCReceiver =
//      new TCPReceiver( sch, codec )

//   @throws( classOf[ IOException ])
//   def withChannel( sch: SocketChannel ) : OSCReceiver =
//      new TCPReceiver( sch, OSCPacketCodec.default )

	protected def debugTimeString : String = {
		new java.text.SimpleDateFormat( "HH:mm:ss.SSS" ).format( new java.util.Date )
	}

   trait Net extends OSCReceiver with OSCChannelNet
}

abstract class OSCReceiver( val localSocketAddress: InetSocketAddress )
extends OSCChannel {
//	private val		collListeners   			= new ArrayList[ OSCListener ]
  	var				action						= (msg: OSCMessage, sender: SocketAddress, time: Long ) => ()
//	protected var	thread : Thread				= null

	protected val	generalSync					= new AnyRef	// mutual exclusion startListening / stopListening
//	protected val	threadSync					= new AnyRef	// communication with receiver thread

//	protected var	listening					= false
   private var    wasClosed               = false
	
//	private val		bufSync						= new AnyRef	// buffer (re)allocation
//	private var		bufSize						= DEFAULTBUFSIZE
//	protected var	byteBuf : ByteBuffer		= null
   protected final val byteBuf	         = ByteBuffer.allocateDirect( config.bufferSize )

	protected var	tgt : SocketAddress			= null

   // ---- constructor ----
//   connect()
//	listening		      = true
	protected val thread = new Thread( "OSCReceiver" ) {
      override def run {
         try {
            receiverLoop()
         } finally {
            generalSync.synchronized {
               wasClosed = true
               generalSync.notifyAll()
            }
         }
      }
   }
	thread.setDaemon( true )
	thread.start()

//   final def transport  = config.transport
//   final def codec      = config.codec
//   final def bufferSize = config.bufferSize

   protected def receiverLoop() : Unit

//	/**
//	 *	Queries the receiver's local socket address.
//	 *	You can determine the host and port from the returned address
//	 *	by calling <code>getHostName()</code> (or for the IP <code>getAddress().getHostAddress()</code>)
//	 *	and <code>getPort()</code>. This port number may be <code>0</code>
//	 *	if the receiver was called with an unspecified port and has not yet been
//	 *	started. In this case, to determine the port actually used, call this
//	 *	method after the receiver has been started.
//	 *	
//	 *	@return				the address of the receiver's local socket.
//	 *
//	 *	@see	java.net.InetSocketAddress#getHostName()
//	 *	@see	java.net.InetSocketAddress#getAddress()
//	 *	@see	java.net.InetSocketAddress#getPort()
//	 */
//	def getLocalAddress : InetSocketAddress

//	var   codec						= OSCPacketCodec.default

//	def getProtocol = protocol

	/**
	 *	Queries the receiver's local socket address.
	 *	You can determine the host and port from the returned address
	 *	by calling <code>getHostName()</code> (or for the IP <code>getAddress().getHostAddress()</code>)
	 *	and <code>getPort()</code>. This port number may be <code>0</code>
	 *	if the receiver was called with an unspecified port and has not yet been
	 *	started. In this case, to determine the port actually used, call this
	 *	method after the receiver has been started.
	 *	
	 *	@return				the address of the receiver's local socket.
	 *
	 *	@see	java.net.InetSocketAddress#getHostName()
	 *	@see	java.net.InetSocketAddress#getAddress()
	 *	@see	java.net.InetSocketAddress#getPort()
	 */
//	def getLocalAddress : InetSocketAddress
//	def localAddress : InetSocketAddress

//	def setTarget( target: SocketAddress ) : Unit
	final def target: SocketAddress = tgt
//	def target_=( t: SocketAddress ) : Unit
	
//	def decoder_=( dec: OSCPacketCodec ) {
//		decoder = dec
//	}
	
//	def getDecoder : OSCPacketCodec = decoder

//	/**
//	 *  Registers a listener that gets informed
//	 *  about incoming messages. You can call this
//	 *  both when listening was started and stopped.
//	 *
//	 *  @param  listener	the listener to register
//	 */
//	def addOSCListener( listener: OSCListener ) {
//		collListeners.synchronized {
//			collListeners.add( listener )
//		}
//	}
//
//	/**
//	 *  Unregisters a listener that gets informed
//	 *  about incoming messages
//	 *
//	 *  @param  listener	the listener to remove from
//	 *						the list of notified objects.
//	 */
//	def removeOSCListener( listener: OSCListener ) {
//		collListeners.synchronized {
//			collListeners.remove( listener )
//		}
//	}

//	/**
//	 *  Starts to wait for incoming messages.
//	 *	See the class constructor description to learn how
//	 *	connected and unconnected channels are handled.
//	 *	You should never modify the
//	 *	the channel's setup between the constructor and calling
//	 *	<code>startListening</code>. This method will check
//	 *	the connection status of the channel, using <code>isConnected</code>
//	 *	and establish the connection if necessary. Therefore,
//	 *	calling <code>connect</code> prior to <code>startListening</code>
//	 *	is not necessary.
//	 *	<p>
//	 *	To find out at which port we are listening, call
//	 *	<code>getLocalAddress().getPort()</code>.
//	 *	<p>
//	 *	If the <code>OSCReceiver</code> is already listening,
//	 *	this method does nothing.
//     *
//     *  @throws IOException when an error occurs
//     *          while establishing the channel connection.
//     *          In that case, no thread has been started
//     *          and hence stopListening() needn't be called
//	 *
//	 *	@throws	IllegalStateException	when trying to call this method from within the OSC receiver thread
//	 *									(which would obviously cause a loop)
//	 */
//	@throws( classOf[ IOException ])
//	def start() {
//		generalSync.synchronized {
//			if( Thread.currentThread == thread ) throw new IllegalStateException( "Cannot be called from reception thread" )
//
//			if( listening && ((thread == null) || !thread.isAlive) ) {
//				listening		= false
//			}
//			if( !listening ) {
//				if( !isConnected ) connect()
//				listening		= true
//				thread			= new Thread( this, "OSCReceiver" )
//				thread.setDaemon( true )
//				thread.start()
//			}
//		}
//	}

	/**
	 *	Queries whether the <code>OSCReceiver</code> is
	 *	listening or not.
	 */
	final def isOpen : Boolean = generalSync.synchronized { !wasClosed }

   protected final def isOpenNoSync : Boolean = !wasClosed

//   protected final def threadTerminates() {
//      generalSync.synchronized {
//         wasClosed = true
//         generalSync.notifyAll()
//      }
//   }

   @throws( classOf[ IOException ])
   final def close() {
      stop()
      closeChannel()
   }

	/**
	 *  Stops waiting for incoming messages. This
	 *	method returns when the receiving thread has terminated.
     *  To prevent deadlocks, this method cancels after
     *  five seconds, calling <code>close()</code> on the datagram
	 *	channel, which causes the listening thread to die because
	 *	of a channel-closing exception.
     *
     *  @throws IOException if an error occurs while shutting down
	 *
	 *	@throws	IllegalStateException	when trying to call this method from within the OSC receiver thread
	 *									(which would obviously cause a loop)
	 */
	@throws( classOf[ IOException ])
	private def stop() {
        generalSync.synchronized {
			if( Thread.currentThread == thread ) throw new IllegalStateException( "Cannot be called from reception thread" )

			if( !wasClosed ) {
				if( thread.isAlive ) {
               try {
//					   threadSync.synchronized {
						   sendGuardSignal()
                     generalSync.wait( 5000 )
//							threadSync.wait( 5000 )
//						}
					}
					catch { case e2: InterruptedException =>
						e2.printStackTrace()
					}
					finally {
						if( !wasClosed && thread.isAlive ) {
							try {
                        Console.err.println( "OSCReceiver.stopListening : rude task killing (" + this.hashCode + ")" )
								closeChannel()
							}
							catch { case e3: IOException =>
								e3.printStackTrace()
							}
						}
                  wasClosed = true
//						thread = null
					}
				}
			}
		}
	}

//	def bufferSize_=( size: Int ) {
//		bufSync.synchronized {
//			if( listening ) throw new IllegalStateException( "Cannot be called while receiver is active" )
//			bufSize	= size
//		}
//	}

//	def dispose() {
//		try {
//			stop()
//		}
//		catch { case e1: IOException =>
//			e1.printStackTrace()
//		}
//		try {
//			closeChannel()
//		}
//		catch { case e1: IOException =>
//			e1.printStackTrace()
//		}
////		collListeners.clear
//		byteBuf	= null
//	}
	
	@throws( classOf[ IOException ])
	protected def sendGuardSignal() : Unit
	
//	@throws( classOf[ IOException ])
//	protected def channel_=( ch: SelectableChannel ) : Unit
// XXX just to make it compile

//	private[ osc ] def channel_=( ch: SelectableChannel ) : Unit
	private[ osc ] def channel: SelectableChannel

	@throws( classOf[ IOException ])
	protected def closeChannel() : Unit

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
   protected final def dumpPacket( p: OSCPacket ) {
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
