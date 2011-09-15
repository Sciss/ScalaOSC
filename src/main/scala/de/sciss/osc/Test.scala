/*
 * Test.scala
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
import java.net.InetAddress
import Implicits._

private[osc] object Test {
//	def codec() {
//// NOTE: scalacheck doesn't seem to be compatible with
////		 scala 2.8 BETA, and i cannot get the sources
//// 		 to compile due to more stupid dependancies.
//// 	     so screw scalacheck for the moment....
///*
//		import _root_.org.scalacheck._
//		import _root_.org.scalacheck.Arbitrary._
//		import _root_.org.scalacheck.Prop._
//
//		var c: PacketCodec = null
//		val b = ByteBuffer.allocate( 8192 )
//		val str = arbitrary[String] suchThat (_.indexOf(0) == -1) // null-character not allowed
//		val strictArgGen = Gen.oneOf( arbitrary[Int], arbitrary[Float], str )
//		val strictListGen = Gen.listOf[Any]( strictArgGen )
//		val fatArgGen = Gen.oneOf( arbitrary[Int], arbitrary[Float], arbitrary[Long], arbitrary[Double], str )
//		val fatListGen = Gen.listOf[Any]( fatArgGen )
//// how do we limit the list size? no clue... seems to work nevertheless
////		val sizedGen = Gen.sized { size => (size < 100) ==> listGen }
//		val checka = (list: List[Any]) => {
//			val msg = Message( "/test", list:_* )
//			b.clear
//			msg.encode( c, b )
//			b.flip
//			val msgOut = c.decode( b ).asInstanceOf[Message]
//			val decArgs = msgOut.args
//			(msgOut.name == msg.name) :| "name" &&
//			(decArgs == msg.args) :| ("args before: " + msg.args + " / after: " + decArgs.toList )
//		}
//		c = new PacketCodec( PacketCodec.MODE_STRICT_V1 )
//		val strictProp = forAll( strictListGen )( checka )
//		strictProp.check
//		c = new PacketCodec( PacketCodec.MODE_FAT_V1 )
//		val fatProp = forAll( fatListGen )( checka )
//		fatProp.check
//*/
//	}
	
   def receiver() {
      println( """
Receiver test

   is waiting for an incoming message
   on UDP port 21327
   Send "/quit" to terminate.
""" )

      val cfg         = UDP.Config()
      cfg.localPort   = 21327  // 0x53 0x4F or 'SO'
      val rcv         = UDP.Receiver( cfg )

      val sync = new AnyRef

      rcv.dump( Dump.Both )
	   rcv.action = {
         case (Message( name, _ @ _* ), _) =>
	    	   println( "Received message '" + name + "'" )
      	   if( name == "/quit" ) sync.synchronized( sync.notifyAll() )
         case (p, addr) => println( "Ignoring: " + p + " from " + addr )
	   }
      rcv.connect()
	   sync.synchronized( sync.wait() )
   }
  
   def transmitter( transport: Transport.Net ) {
      println(
"""Transmitter tests
   assume that scsynth is running on
   localhost """ + transport.name + """ port 57110
""" )

      val tgt  = InetAddress.getLocalHost -> 57110
      val trns = transport match {
         case UDP => UDP.Transmitter( tgt )
         case TCP => TCP.Transmitter( tgt )
      }

      try {
         trns.dump( stream = Console.out )
         trns.connect()
//println( trns.target )
         trns ! Message( "/s_new", "default", 1000, 0, 0, "amp", 0f )
         for( i <- (1 to 8) ) {
        	   trns ! Message( "/n_set", 1000, "freq", i * 333, "amp", 0.5f )
        	   Thread.sleep( 200 )
         }

         import de.sciss.osc.{ Message => M }
         trns ! M( "/n_free", 1000 )

      } catch {
        case e1: InterruptedException => ()
        case e2: IOException =>
          println( e2.getClass.getName + " : " + e2.getLocalizedMessage )
      }
      finally {
         trns.close()
      }
   }

   def tcpClient() {
      val c: Client = sys.error( "TODO" ) // = Client( TCP, loopBack = true )
//      c.target = new InetSocketAddress( "127.0.0.1", 57110 )
//      c.start()
      c.dump()
//      c ! Message( "/dumpOSC", 1 )
//      c ! Message( "/notify", 1 )
   }
}
