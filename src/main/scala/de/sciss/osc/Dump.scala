/*
 * Dump.scala
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

object Dump {
   def apply( id: Int ) : Dump = id match {
      case Off.id    => Off
      case Text.id   => Text
      case Hex.id    => Hex
      case Both.id   => Both
      case _         => throw new IllegalArgumentException( id.toString )
   }

   /**
    *	Dump mode: do not dump messages
    */
   case object Off extends Dump  { val id = 0 }
   /**
    *	Dump mode: dump messages in text formatting
    */
   case object Text extends Dump { val id = 1 }
   /**
    *	Dump mode: dump messages in hex (binary) view
    */
   case object Hex extends Dump  { val id = 2 }
   /**
    *	Dump mode: dump messages both in text and hex view
    */
   case object Both extends Dump { val id = 3 }

   type Filter = Packet => Boolean
   val AllPackets : Filter = _ => true
}
sealed trait Dump { val id: Int }