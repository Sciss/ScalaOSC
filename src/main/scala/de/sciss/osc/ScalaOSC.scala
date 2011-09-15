/*
 * ScalaOSC.scala
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

object ScalaOSC {
   val name          = "ScalaOSC"
   val version       = 0.24
   val copyright     = "(C)opyright 2008-2011 Hanns Holger Rutz"

   def versionString = (version + 0.001).toString.substring( 0, 4 )

   def main( args: Array[ String ]) {
	   args.toSeq match {
         case Seq( "--testTransmitter", transName ) =>
            OSCTransport( transName ) match {
               case netTrans: OSCTransport.Net => Test.transmitter( netTrans )
               case _ => sys.error( "Unsupported transport '" + transName + "'" )
            }
         case Seq( "--testReceiver", transName ) =>
            OSCTransport( transName ) match {
               case netTrans: OSCTransport.Net => Test.receiver( netTrans )
               case _ => sys.error( "Unsupported transport '" + transName + "'" )
            }
//         case Seq( "--runChecks" ) =>
//            Test.codec()
         case Seq( "--testTCPClient" ) =>
            Test.tcpClient()
         case _ =>
            printInfo()

            println( """" +
The following demos are available:

   --testTransmitter (UDP|TCP)"
   --testReceiver (UDP|TCP)"

""" )
            sys.exit( 1 )
		}
	}

   def printInfo() {
      println( "\n" + name + " v" + versionString + "\n" + copyright +
         ". All rights reserved.\n\nThis is a library which cannot be executed directly.\n" )
   }

	/**
	 *	Returns a license and website information string
	 *	about the library
	 *
	 *	@return	text string which can be displayed
	 *			in an about box
	 */
	val credits = """This library is released under the GNU Lesser General Public License.
All software provided "as is", no warranties, no liability!
For project status visit http://www.sciss.de/scalaOSC."""
}
