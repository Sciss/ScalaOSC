/*
 * Timetag.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2013 Hanns Holger Rutz. All rights reserved.
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

import java.util.Locale
import java.text.{DecimalFormat, NumberFormat, SimpleDateFormat}

object Timetag {
   /**
    *  The special timetag value
    *  to indicate that the bundle be
    *  processed as soon as possible
    */
   val now = Timetag( 1 )

   /**
    * Converts a relative time value in seconds, as required
    * for example for scsynth offline rendering, into a raw timetag.
    */
   def secs( delta: Double ) = Timetag( (delta.toLong << 32) + ((delta % 1.0) * 0x100000000L + 0.5).toLong )

   private val datef    = new SimpleDateFormat( "HH:mm:ss.SSS", Locale.US )
   private val decimf   = {
      val res = NumberFormat.getInstance( Locale.US )
      res match {
         case d: DecimalFormat => {
            d.setGroupingUsed( false )
            d.setMinimumFractionDigits( 1 )
            d.setMaximumFractionDigits( 5 )
         }
         case _ =>
      }
      res
   }

   /**
    * Converts a time value from the system clock value in milliseconds since
    * jan 1 1970, as returned by System.currentTimeMillis, into a raw timetag.
    */
   def millis( abs: Long ) : Timetag = {
      val secsSince1900    = abs / 1000 + SECONDS_FROM_1900_TO_1970
      val secsFractional	= (((abs % 1000) << 32) + 500) / 1000
      Timetag( (secsSince1900 << 32) | secsFractional )
   }

   private val SECONDS_FROM_1900_TO_1970 = 2208988800L
}
final case class Timetag( raw: Long ) {
   /**
    * Converts the raw timetag into a time value from the system clock value in milliseconds since
    * jan 1 1970, corresponding to what is returned by System.currentTimeMillis.
    */
   def toMillis : Long = {
      val m1 = ((raw & 0xFFFFFFFFL) * 1000) >> 32
      val m2 = (((raw >> 32) & 0xFFFFFFFFL) - Timetag.SECONDS_FROM_1900_TO_1970) * 1000
      m1 + m2
   }

   /**
    * Converts a raw timetag into a relative time value in seconds, as required
    * for example for scsynth offline rendering. In general, this will return
    * the amount of seconds since midnight on January 1, 1900, as defined by
    * the OSC standard.
    */
   def toSecs : Double = {
      val frac = (raw & 0xFFFFFFFFL).toDouble / 0x100000000L
      val secs = (raw >> 32).toDouble
      secs + frac
   }

   override def toString : String = {
      if( raw == 1 ) "<now>" else {
         val secsSince1900 = (raw >> 32) & 0xFFFFFFFFL
         if( secsSince1900 > Timetag.SECONDS_FROM_1900_TO_1970 ) {
            Timetag.datef.format( toMillis )
         } else {
            Timetag.decimf.format( toSecs )
         }
      }
   }
}