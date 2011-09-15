/*
 * OSCClient.scala
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

import java.io.{ IOException, PrintStream }
import java.net.{ InetSocketAddress, SocketAddress }

/**
 *	This class groups together a transmitter and receiver, allowing bidirectional
 *	OSC communication from the perspective of a client. It simplifies the
 *	need to use several objects by uniting their functionality.
 *	</P><P>
 *	In the following example, a client for UDP to SuperCollider server (scsynth)
 *	on the local machine is created. The client starts a synth by sending
 *	a <code>/s_new</code> message, and stops the synth by sending a delayed
 *	a <code>/n_set</code> message. It waits for the synth to die which is recognized
 *	by an incoming <code>/n_end</code> message from scsynth after we've registered
 *	using a <code>/notify</code> command.
 *
 *	<pre>
    final Object        sync = new Object();
    final OSCClient     c;
    final OSCBundle     bndl1, bndl2;
    final Integer       nodeID;
    
    try {
        c = OSCClient.newUsing( OSCClient.UDP );    // create UDP client with any free port number
        c.setTarget( new InetSocketAddress( "127.0.0.1", 57110 ));  // talk to scsynth on the same machine
        c.start();  // open channel and (in the case of TCP) connect, then start listening for replies
    }
    catch( IOException e1 ) {
        e1.printStackTrace();
        return;
    }
    
    // register a listener for incoming osc messages
    c.addOSCListener( new OSCListener() {
        public void messageReceived( OSCMessage m, SocketAddress addr, long time )
        {
            // if we get the /n_end message, wake up the main thread
            // ; note: we should better also check for the node ID to make sure
            // the message corresponds to our synth
            if( m.getName().equals( "/n_end" )) {
                synchronized( sync ) {
                    sync.notifyAll();
                }
            }
        }
    });
    // let's see what's going out and coming in
    c.dumpOSC( OSCChannel.kDumpBoth, Console.err );
    
    try {
        // the /notify message tells scsynth to send info messages back to us
        c.send( new OSCMessage( "/notify", new Object[] { new Integer( 1 )}));
        // two bundles, one immediately (with 50ms delay), the other in 1.5 seconds
        bndl1   = new OSCBundle( System.currentTimeMillis() + 50 );
        bndl2   = new OSCBundle( System.currentTimeMillis() + 1550 );
        // this is going to be the node ID of our synth
        nodeID  = new Integer( 1001 + i );
        // this next messages creates the synth
        bndl1.addPacket( new OSCMessage( "/s_new", new Object[] { "default", nodeID, new Integer( 1 ), new Integer( 0 )}));
        // this next messages starts to releases the synth in 1.5 seconds (release time is 2 seconds)
        bndl2.addPacket( new OSCMessage( "/n_set", new Object[] { nodeID, "gate", new Float( -(2f + 1f) )}));
        // send both bundles (scsynth handles their respective timetags)
        c.send( bndl1 );
        c.send( bndl2 );

        // now wait for the signal from our osc listener (or timeout in 10 seconds)
        synchronized( sync ) {
            sync.wait( 10000 );
        }
        catch( InterruptedException e1 ) {}
        
        // ok, unsubscribe getting info messages
        c.send( new OSCMessage( "/notify", new Object[] { new Integer( 0 )}));

        // ok, stop the client
        // ; this isn't really necessary as we call dispose soon
        c.stop();
    }
    catch( IOException e11 ) {
        e11.printStackTrace();
    }
    
    // dispose the client (it gets stopped if still running)
    c.dispose();
 *	</pre>
 *
 *	@see		OSCTransmitter
 *	@see		OSCReceiver
 *	@see		OSCServer
 *
 *  @version	0.37, 27-May-10
 */
object OSCClient {
	/**
	 *	Creates a new instance of an <code>OSCClient</code>, using
	 *	a specific codec and transport protocol and port. It
	 *	uses the local machine's IP or the &quot;loopback&quot; address.
	 *	<p>
	 *	Note that the <code>port</code> specifies the
	 *	local socket (at which the client listens and from which it sends),
	 *	it does not determine the remote sockets from which messages can be received
	 *	and to which messages are sent. The target socket can be set
	 *	using the <code>setTarget</code> method!
	 *
    *	@param	transport the protocol to use, currently either <code>UDP</code> or <code>TCP</code>
    *	@param	port		the port number for the OSC socket, or <code>0</code> to use an arbitrary free port
	 *	@param	codec		the codec to use
	 *	@param	loopBack	if <code>true</code>, the &quot;loopback&quot; address (<code>&quot;127.0.0.1&quot;</code>)
	 *						is used which limits communication to the local machine. If <code>false</code>, the
	 *						special IP <code>"0.0.0.0"</code> is used which means messages from any IP as well as from
	 *						the loopback are accepted
	 *	
	 *	@return				the newly created client
	 *
	 *	@throws	IOException					if a networking error occurs while creating the socket
	 *	@throws	IllegalArgumentException	if an illegal protocol is used
	 */
//   @throws( classOf[ IOException ])
//   def apply( transport: OSCTransport, port: Int = 0, loopBack: Boolean = false,
//              codec: OSCPacketCodec = OSCPacketCodec.default ) : OSCClient = {
//	   val rcv  = OSCReceiver( transport, port, loopBack, codec )
//   	val trns	= OSCTransmitter( transport, port, loopBack, codec )
//
//	   new OSCClient( rcv, trns, transport )
//   }

//   @throws( classOf[ IOException ])
//   def withAddress( transport: OSCTransport, localAddress: InetSocketAddress,
//                    codec: OSCPacketCodec = OSCPacketCodec.default ) : OSCClient = {
//	   val rcv  = OSCReceiver.withAddress( transport, localAddress, codec )
//   	val trns	= OSCTransmitter.withAddress( transport, localAddress, codec )
//
//	   new OSCClient( rcv, trns, transport )
//   }
}

trait OSCClient extends OSCInputChannel with OSCOutputChannel {
	import OSCChannel._
	
//	private var bufSize = DEFAULTBUFSIZE

   protected def rcv: OSCReceiver
   protected def trns: OSCTransmitter.Directed

	def action_=( f: (OSCMessage, SocketAddress, Long) => Unit ) {
		rcv.action = f
	}
	def action: (OSCMessage, SocketAddress, Long) => Unit = rcv.action

	/**
	 *	Queries the client side socket address. This is the address
	 *	from which the client sends and at which it listens for replies.
	 *	You can determine the host and port from the returned address
	 *	by calling <code>getHostName()</code> (or for the IP <code>getAddress().getHostAddress()</code>)
	 *	and <code>getPort()</code>.
	 *	<p>
	 *	Note that if the client is bound to the accept-any IP <code>"0.0.0.0"</code>,
	 *	which happens for example when calling <code>newUsing( &lt;transport&gt;, 0, false )</code>,
	 *	the returned IP will be the localhost's IP, so you can
	 *	patch the result directly into any <code>setTarget</code> call.
	 *	
	 *	@return				the address of the client's local socket.
	 *
	 *	@throws	IOException	if the local host could not be resolved
	 *
	 *	@see	java.net.InetSocketAddress#getHostName()
	 *	@see	java.net.InetSocketAddress#getAddress()
	 *	@see	java.net.InetSocketAddress#getPort()
	 *
	 *	@see	#getProtocol()
	 */
//	@throws( classOf[ IOException ])
//	def localAddress = rcv.localAddress

   final def localSocketAddress = rcv.localSocketAddress

//	/**
//	 *	Specifies the client's target address, that is the address of the server to talk to.
//	 *	You should call this method only once and you must call it before starting the client
//	 *	or sending messages.
//	 *
//	 *	@param	target	the address of the server. Usually you construct an appropriate <code>InetSocketAddress</code>
//	 *
//	 *	@see	InetSocketAddress
//	 */
//	def target_=( target: SocketAddress ) {
//		rcv.target  = target
//		trns.target = target
//	}
	def target: SocketAddress = rcv.target
	
//	def codec_=( c: OSCPacketCodec ) {
//		rcv.codec	= c
//		trns.codec	= c
//	}
	
//	def codec: OSCPacketCodec = rcv.codec
	
//	/**
//	 *	Initializes network channel (if necessary) and establishes connection for transports requiring
//	 *	connectivity (e.g. TCP). Do not call this method when the client is already connected.
//	 *	Note that <code>start</code> implicitly calls <code>connect</code> if necessary, so
//	 *	usually you will not need to call <code>connect</code> yourself.
//	 *
//	 *	@throws	IOException	if a networking error occurs. Possible reasons: - the underlying
//	 *						network channel had been closed by the server. - the transport
//	 *						is TCP and the server is not available. - the transport is TCP
//	 *						and the client was stopped before (unable to revive).
//	 *
//	 *	@see	#isConnected()
//	 *	@see	#start()
//	 */
//	@throws( classOf[ IOException ])
//	def connect() { trns.connect() }

//	/**
//	 *	Queries the connection state of the client.
//	 *
//	 *	@return	<code>true</code> if the client is connected, <code>false</code> otherwise. For transports that do not use
//	 *			connectivity (e.g. UDP) this returns <code>false</code>, if the
//	 *			underlying <code>DatagramChannel</code> has not yet been created.
//	 *
//	 *	@see	#connect()
//	 */
//	def isConnected: Boolean = trns.isConnected
	
	/**
	 *	Sends an OSC packet (bundle or message) to the target
	 *	network address. Make sure that the client's target
	 *	has been specified before by calling <code>setTarget()</code>
	 *
	 *	@param	p		the packet to send
	 *
	 *	@throws	IOException				if a write error, OSC encoding error,
	 *									buffer overflow error or network error occurs,
	 *									for example if a TCP client was not connected before.
	 *	@throws	NullPointerException	for a UDP client if the target has not been specified
	 *
	 *	@see	#setTarget( SocketAddress )
	 */
	@throws( classOf[ IOException ])
	def !( p: OSCPacket ) { trns.!( p )}

	/**
	 *  Registers a listener that gets informed
	 *  about incoming messages. You can call this
	 *  both when the client is active or inactive.
	 *
	 *  @param  listener	the listener to register
	 */
//	public void addOSCListener( OSCListener listener )
//	{
//		rcv.addOSCListener( listener );
//	}

	/**
	 *  Unregisters a listener that gets informed
	 *  about incoming messages
	 *
	 *  @param  listener	the listener to remove from
	 *						the list of notified objects.
	 */
//	public void removeOSCListener( OSCListener listener )
//	{
//		rcv.removeOSCListener( listener );
//	}

//	/**
//	 *	Starts the client. This calls <code>connect</code> if the transport requires
//	 *	connectivity (e.g. TCP) and the channel is not yet connected.
//	 *	It then tells the underlying OSC receiver to start listening.
//	 *
//	 *	@throws	IOException	if a networking error occurs. Possible reasons: - the underlying
//	 *						network channel had been closed by the server. - the transport
//	 *						is TCP and the server is not available. - the transport is TCP
//	 *						and the client was stopped before (unable to revive).
//	 *
//	 *	@warning	in the current version, it is not possible to &quot;revive&quot;
//	 *				clients after the server has closed the connection. Also it's not
//	 *				possible to start a TCP client more than once. This might be
//	 *				possible in a future version.
//	 */
//	@throws( classOf[ IOException ])
//	def start() {
//		if( !trns.isConnected ) {
//			trns.connect()
//			rcv.channel = trns.channel
//		}
//		rcv.start()
//	}
	
	/**
	 *	Queries whether the client was activated or not. A client is activated by
	 *	calling its <code>start()</code> method and deactivated by calling <code>stop()</code>.
	 *
	 *	@return	<code>true</code> if the client is active (connected and listening), <code>false</code> otherwise.
	 *
	 *	@see	#start()
	 *	@see	#stop()
	 */
	def isOpen: Boolean = rcv.isOpen

	@throws( classOf[ IOException ])
	def close() {
      rcv.close()
      trns.close()
   }

//	/**
//	 *	Adjusts the buffer size for OSC messages (both for sending and receiving).
//	 *	This is the maximum size an OSC packet (bundle or message) can grow to.
//	 *	The initial buffer size is <code>DEFAULTBUFSIZE</code>. Do not call this
//	 *	method while the client is active!
//	 *
//	 *	@param	size					the new size in bytes.
//	 *
//	 *	@throws	IllegalStateException	if trying to change the buffer size while the client is active
//	 *									(listening).
//	 *
//	 *	@see	#isActive()
//	 *	@see	#getBufferSize()
//	 */
//	def bufferSize_=( size: Int ) {
//		bufSize			= size
//		rcv.bufferSize	= size
//		trns.bufferSize	= size
//	}
	
	/**
	 *	Queries the buffer size used for sending and receiving OSC messages.
	 *	This is the maximum size an OSC packet (bundle or message) can grow to.
	 *	The initial buffer size is <code>DEFAULTBUFSIZE</code>.
	 *
	 *	@return			the buffer size in bytes.
	 *
	 *	@see	#setBufferSize( int )
	 */
//	def bufferSize: Int = bufSize

	/**
	 *	Changes the way incoming and outgoing OSC messages are printed to the standard err console.
	 *	By default messages are not printed.
	 *
	 *  @param	mode	one of <code>kDumpOff</code> (don't dump, default),
	 *					<code>kDumpText</code> (dump human readable string),
	 *					<code>kDumpHex</code> (hexdump), or
	 *					<code>kDumpBoth</code> (both text and hex)
	 *	@param	stream	the stream to print on
	 *
	 *	@see	#dumpIncomingOSC( int, PrintStream )
	 *	@see	#dumpOutgoingOSC( int, PrintStream )
	 *	@see	#kDumpOff
	 *	@see	#kDumpText
	 *	@see	#kDumpHex
	 *	@see	#kDumpBoth
	 */
	override def dumpOSC( mode: OSCDump = OSCDump.Text,
					          stream: PrintStream = Console.err,
					          filter: (OSCPacket) => Boolean = PassAllPackets ) {
		dumpIncomingOSC( mode, stream, filter )
		dumpOutgoingOSC( mode, stream, filter )
	}

	def dumpIncomingOSC( mode: OSCDump = OSCDump.Text,
					         stream: PrintStream = Console.err,
					         filter: (OSCPacket) => Boolean = PassAllPackets ) {

		rcv.dumpOSC( mode, stream, filter )
	}
	
	def dumpOutgoingOSC( mode: OSCDump = OSCDump.Text,
					         stream: PrintStream = Console.err,
					         filter: (OSCPacket) => Boolean = PassAllPackets ) {

		trns.dumpOSC( mode, stream, filter )
	}

//	/**
//	 *	Destroys the client and frees resources associated with it.
//	 *	This automatically stops the client and closes the networking channel.
//	 *	Do not use this client instance any more after calling <code>dispose.</code>
//	 */
//	def dispose() {
//		rcv.dispose()
//		trns.dispose()
//	}
}