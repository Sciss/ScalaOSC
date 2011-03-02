/*
 *  ScalaOSC.scala
 *  (ScalaOSC)
 *
 *  Copyright (c) 2008-2011 Hanns Holger Rutz. All rights reserved.
 *
 *	 This library is free software; you can redistribute it and/or
 *	 modify it under the terms of the GNU Lesser General Public
 *	 License as published by the Free Software Foundation; either
 *	 version 2.1 of the License, or (at your option) any later version.
 *
 *	 This library is distributed in the hope that it will be useful,
 *	 but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	 Lesser General Public License for more details.
 *
 *	 Below is a copy of the GNU Lesser General Public License
 *
 *	 For further information, please contact Hanns Holger Rutz at
 *	 contact@sciss.de
 */

package de.sciss.osc

import java.util.{ MissingResourceException, ResourceBundle }

/**
 *    @version 0.19, 10-Aug-10
 */
object ScalaOSC {
   val name                = "ScalaOSC"
   val version             = 0.22
   val copyright           = "(C)opyright 2008-2011 Hanns Holger Rutz"
//   private val resBundle	= ResourceBundle.getBundle( "ScalaOSCStrings" )

   def versionString = (version + 0.001).toString.substring( 0, 4 )

   def main( args: Array[ String ]) {
	   var testo = false
	
		if( args.length == 1 ) {
		   args( 0 ) match {
		      case "--testTransmitter" => {
				   testo	= true
				   Test.transmitter
			   }
		      case "--testReceiver" => {
		    	   testo = true
		    	   Test.receiver
		      }
		      case "--runChecks" => {
		    	   testo = true
		    	   Test.codec
		      }
            case "--testTCPClient" => {
               testo = true
               Test.tcpClient
            }
		   }
		}

		if( !testo ) {
         printInfo

			System.out.println( "\nThe following demos are available:\n" +
				"  --testTransmitter\n"
			)
			System.exit( 1 )
		}
	}

   def printInfo {
      println( "\n" + name + " v" + versionString + "\n" + copyright +
         ". All rights reserved.\n\nThis is a library which cannot be executed directly.\n" )
   }
   
// 	/**
//	 *	Returns the library's version.
//	 *
//	 *	@return	the current version of NetUtil
//	 */
//	def getVersion = VERSION

//	/**
//	 *	Returns a copyright information string
//	 *	about the library
//	 *
//	 *	@return	text string which can be displayed
//	 *			in an about box
//	 */
//	def getCopyrightString = getResourceString( "copyright" )

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


//	/**
//	 *	Returns a string from the library's string
//	 *	resource bundle (currently localized
//	 *	english and german). This is used by the
//	 *	classes of the library, you shouldn't use
//	 *	it yourself.
//	 *
//	 *	@param	key	lookup dictionary key
//	 *	@return	(localized) human readable string for
//	 *			the given key or placeholder string if
//	 *			the resource wasn't found
//	 */
//	private[scalaosc] def getResourceString( key: String ) : String = {
//		try {
//			resBundle.getString( key )
//		}
//		catch { case e: MissingResourceException =>
//			"[" + key + "]"
//		}
//	}
}
