/*
 * Client.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2012 Hanns Holger Rutz. All rights reserved.
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
    final Client     c;
    final Bundle     bndl1, bndl2;
    final Integer       nodeID;
    
    try {
        c = Client.newUsing( Client.UDP );    // create UDP client with any free port number
        c.setTarget( new InetSocketAddress( "127.0.0.1", 57110 ));  // talk to scsynth on the same machine
        c.start();  // open channel and (in the case of TCP) connect, then start listening for replies
    }
    catch( IOException e1 ) {
        e1.printStackTrace();
        return;
    }
    
    // register a listener for incoming osc messages
    c.addOSCListener( new OSCListener() {
        public void messageReceived( Message m, SocketAddress addr, long time )
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
    c.dumpOSC( Channel.kDumpBoth, Console.err );
    
    try {
        // the /notify message tells scsynth to send info messages back to us
        c.send( new Message( "/notify", new Object[] { new Integer( 1 )}));
        // two bundles, one immediately (with 50ms delay), the other in 1.5 seconds
        bndl1   = new Bundle( System.currentTimeMillis() + 50 );
        bndl2   = new Bundle( System.currentTimeMillis() + 1550 );
        // this is going to be the node ID of our synth
        nodeID  = new Integer( 1001 + i );
        // this next messages creates the synth
        bndl1.addPacket( new Message( "/s_new", new Object[] { "default", nodeID, new Integer( 1 ), new Integer( 0 )}));
        // this next messages starts to releases the synth in 1.5 seconds (release time is 2 seconds)
        bndl2.addPacket( new Message( "/n_set", new Object[] { nodeID, "gate", new Float( -(2f + 1f) )}));
        // send both bundles (scsynth handles their respective timetags)
        c.send( bndl1 );
        c.send( bndl2 );

        // now wait for the signal from our osc listener (or timeout in 10 seconds)
        synchronized( sync ) {
            sync.wait( 10000 );
        }
        catch( InterruptedException e1 ) {}
        
        // ok, unsubscribe getting info messages
        c.send( new Message( "/notify", new Object[] { new Integer( 0 )}));

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
 *	@see		Transmitter
 *	@see		Receiver
 *	@see		OSCServer
 */
object Client

trait Client extends Channel.Bidi with Channel.DirectedInput with Channel.DirectedOutput {
//   var action = Channel.DirectedInput.NoAction

   override protected def input: Receiver
   override protected def output: Transmitter.Directed

//	/**
//	 *	Sends an OSC packet (bundle or message) to the target
//	 *	network address. Make sure that the client's target
//	 *	has been specified before by calling <code>setTarget()</code>
//	 *
//	 *	@param	p		the packet to send
//	 *
//	 *	@throws	IOException				if a write error, OSC encoding error,
//	 *									buffer overflow error or network error occurs,
//	 *									for example if a TCP client was not connected before.
//	 *	@throws	NullPointerException	for a UDP client if the target has not been specified
//	 *
//	 *	@see	#setTarget( SocketAddress )
//	 */
//	@throws( classOf[ IOException ])
//	def !( p: Packet ) { trns.!( p )}

	@throws( classOf[ IOException ])
	final def close() {
      input.close()
      output.close()
   }
}