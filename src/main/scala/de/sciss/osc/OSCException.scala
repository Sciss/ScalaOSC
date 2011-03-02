/*
 *  OSCException.scala
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

import java.io.IOException
import ScalaOSC._

object OSCException {
	private val errMessages = Array(
        "OSC Communication Timeout", "OSC Command Failed", "OSC Buffer Overflow or Underflow",
        "Illegal OSC Message Format", "Unsupported OSC Type Tag", "OSC message cannot contain argument of class",
        "Error while receiving OSC packet"
    )
 
    /**
	 *  causeType : communication timeout
	 */
	val TIMEOUT = 0
	/**
	 *  causeType : buffer overflow or underflow
	 */
	val BUFFER  = 1
	/**
	 *  causeType : osc message has invalid format
	 */
	val ENCODE  = 2
	/**
	 *  causeType : osc message has invalid or unsupported type tags
	 */
	val DECODE  = 3
	/**
	 *  causeType : network error while receiving osc message
	 */
	val RECEIVE  = 4
 
	def getMessage( causeType: Int, message: String = null ) =
			 errMessages( causeType ) + (if( message == null ) "" else (": " + message))
}

/**
 *  Exception thrown by some OSC related methods.
 *  Typical reasons are communication timeout and
 *  buffer underflows or overflows.
 *
 *  @version	0.11, 27-May-10
 */
class OSCException( val causeType: Int, message: String )
extends IOException( OSCException.getMessage( causeType, message )) {
  	
//	/**
//	 *  Queries the cause of the exception
//	 *
//	 *  @return cause of the exception, e.g. <code>BUFFER</code>
//	 *			if a buffer underflow or overflow occured
//	 */
//	def getCauseType = causeType
	
	override def getLocalizedMessage : String = getMessage
}