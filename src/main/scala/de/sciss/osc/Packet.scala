/*
 * OSCPacket.scala
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

import java.io.PrintStream
import java.nio.{ BufferOverflowException, BufferUnderflowException, ByteBuffer }
import collection.LinearSeqLike
import collection.mutable.Builder
import java.text.{NumberFormat, SimpleDateFormat, DecimalFormat}
import java.util.Locale

object Packet {
	private val HEX   = "0123456789ABCDEF".getBytes
  	private val PAD   = new Array[ Byte ]( 4 )
 
	/**
	 *	Prints a text version of a packet to a given stream.
	 *	The format is similar to scsynth using dump mode 1.
	 *	Bundles will be printed with each message on a separate
	 *	line and increasing indent.
	 *
	 *	@param	stream   the print stream to use, for example <code>System.out</code>
	 *	@param	p        the packet to print (either a message or bundle)
	 */
	def printTextOn( p: Packet, c: PacketCodec, stream: PrintStream ) {
		p.printTextOn( c, stream, 0 )
	}

	/**
	 *	Prints a hexdump version of a packet to a given stream.
	 *	The format is similar to scsynth using dump mode 2.
	 *	Unlike <code>printTextOn</code> this takes a raw received
	 *	or encoded byte buffer and not a decoded instance
	 *	of <code>Packet</code>.
	 *
	 *	@param	stream	the print stream to use, for example <code>System.out</code>
	 *	@param	b		   the byte buffer containing the packet. the current position
    *	                  is saved, and the printing is performed from position 0 to
    *	                  the limit of the buffer. the previous position is restored.
	 *
	 *	@see	java.nio.Buffer#limit()
	 *	@see	java.nio.Buffer#position()
	 */
   def printHexOn( b: ByteBuffer, stream: PrintStream ) {
      val pos0 = b.position
      try {
         val lim	= b.limit
         val txt	= new Array[ Byte ]( 74 )

         var j = 0
         var k = 0
         var m = 0
         var n = 0
         var i = 4
         while( i < 56 ) {
            txt( i ) = 0x20.toByte
            i += 1
         }
         txt( 56 ) = 0x7C.toByte

         stream.println()
         b.position( 0 )
         i = 0; while( i < lim ) {
            j = 0
            txt( j )	= HEX( (i >> 12) & 0xF ); j += 1
            txt( j )	= HEX( (i >> 8) & 0xF ); j += 1
            txt( j )	= HEX( (i >> 4) & 0xF ); j += 1
            txt( j )	= HEX( i & 0xF ); j += 1
            m = 57
            k = 0
            while( (k < 16) && (i < lim) ) {
               j += (if( (k & 7) == 0 ) 2 else 1)
               n			= b.get()
               txt( j )	= HEX( (n >> 4) & 0xF ); j += 1
               txt( j )	= HEX( n & 0xF ); j += 1
               txt( m )	= (if( (n > 0x1F) && (n < 0x7F) ) n.toByte else 0x2E.toByte); m += 1
               k += 1
               i += 1
            }
            txt( m ) = 0x7C.toByte; m += 1
            while( j < 54 ) {
               txt( j ) = 0x20.toByte; j += 1
            }
            while( m < 74 ) {
               txt( m ) = 0x20.toByte; m += 1
            }
            stream.write( txt, 0, 74 )
            stream.println()
         }
         stream.println()
      } finally {
         b.position( pos0 )
      }
   }
	
   def printEscapedStringOn( stream: PrintStream, str: String ) {
      stream.print( '\"' )
      val numChars = str.length
      var i = 0
      while( i < numChars ) {
         val ch = str.charAt( i )
         stream.print(
            if( ch >= 32 ) {
               if( ch < 0x80 ) {
                  if( ch == '"' ) "\\\"" else if( ch == '\\' ) "\\\\" else ch
               } else {
                  (if( ch < 0x100 ) "\\u00" else if( ch < 0x1000) "\\u0" else "\\u") +
                  Integer.toHexString( ch ).toUpperCase
               }
            } else {
               ch match {
                     case '\b' => "\\b"
                     case '\n' => "\\n"
                     case '\t' => "\\t"
                     case '\f' => "\\f"
                     case '\r' => "\\r"
                     case _ => (if( ch > 0xF) "\\u00" else "\\u000") +
                        Integer.toHexString( ch ).toUpperCase
               }
            }
         )
         i += 1
      }
      stream.print( '\"' )
   }

	/**
	 *  Reads a null terminated string from
	 *  the current buffer position
	 *
	 *  @param  b   buffer to read from. position and limit must be
	 *				set appropriately. new position will be right after
	 *				the terminating zero byte when the method returns
	 *  
	 *  @throws BufferUnderflowException	in case the string exceeds
	 *										the provided buffer limit
	 */
	@throws( classOf[ BufferUnderflowException ]) 
	def readString( b: ByteBuffer ) : String = {
		val pos = b.position
		while( b.get != 0 ) ()
		val len = b.position - pos
		val bytes = new Array[ Byte ]( len )
		b.position( pos )
		b.get( bytes )
		new String( bytes, 0, len - 1 )
	}

	/**
	 *  Adds as many zero padding bytes as necessary to
	 *  stop on a 4 byte alignment. if the buffer position
	 *  is already on a 4 byte alignment when calling this
	 *  function, another 4 zero padding bytes are added.
	 *  buffer position will be on the new aligned boundary
	 *  when return from this method
	 *
	 *  @param  b   the buffer to pad
	 *  
	 *  @throws BufferOverflowException		in case the padding exceeds
	 *										the provided buffer limit
	 */
	@throws( classOf[ BufferOverflowException ]) 
	def terminateAndPadToAlign( b: ByteBuffer ) {
		b.put( PAD, 0, 4 - (b.position & 0x03) )
	}
	
	/**
	 *  Adds as many zero padding bytes as necessary to
	 *  stop on a 4 byte alignment. if the buffer position
	 *  is already on a 4 byte alignment when calling this
	 *  function, this method does nothing.
	 *
	 *  @param  b   the buffer to align
	 *  
	 *  @throws BufferOverflowException		in case the padding exceeds
	 *										the provided buffer limit
	 */
	@throws( classOf[ BufferOverflowException ]) 
	def padToAlign( b: ByteBuffer ) {
		b.put( PAD, 0, -b.position & 0x03 )  // nearest 4-align
	}

	/**
	 *  Advances in the buffer as long there
	 *  are non-zero bytes, then advance to a
	 *  four byte alignment.
	 *
	 *  @param  b   the buffer to advance
	 *  
	 *  @throws BufferUnderflowException	in case the reads exceed
	 *										the provided buffer limit
	 */
	@throws( classOf[ BufferUnderflowException ]) 
	def skipToValues( b: ByteBuffer ) {
		while( b.get != 0x00 ) ()
		val newPos = (b.position + 3) & ~3
		if( newPos > b.limit ) throw new BufferUnderflowException
		b.position( newPos )
	}

	/**
	 *  Advances the current buffer position
	 *  to an integer of four bytes. The position
	 *  is not altered if it is already
	 *  aligned to a four byte boundary.
	 *  
	 *  @param  b   the buffer to advance
	 *  
	 *  @throws BufferUnderflowException	in case the skipping exceeds
	 *										the provided buffer limit
	 */
	@throws( classOf[ BufferUnderflowException ]) 
   def skipToAlign( b: ByteBuffer ) {
      val newPos = (b.position + 3) & ~3
      if( newPos > b.limit ) throw new BufferUnderflowException
      b.position( newPos )
   }

   object Atom {
      import scala.{Boolean => SBoolean, Byte => SByte, Double => SDouble, Float => SFloat, Int => SInt, Long => SLong,
         None => SNone, Symbol => SSymbol}
      import java.lang.{String => SString}
      import de.sciss.osc.{Packet => OSCPacket, Timetag => OSCTimetag}

      private def errUnsupported( text: String ) = throw PacketCodec.UnsupportedAtom( text )

      trait Encoder[ @specialized A ] {
         def encode( c: PacketCodec, v: A, tb: ByteBuffer, db: ByteBuffer ) : Unit
         def getEncodedSize( c: PacketCodec, v: A ) : Int
         def printTextOn( c: PacketCodec, v: A, stream: PrintStream, nestCount: Int ) { stream.print( v )}
      }

      trait Decoder[ @specialized A ] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) : A
      }

      object Int extends Atom[SInt] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = b.getInt()

         def encode( c: PacketCodec, v: SInt, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x69.toByte )	// 'i'
            db.putInt( v )
         }

         def getEncodedSize( c: PacketCodec, v: Int ) = 4
      }

      object Float extends Atom[SFloat] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = b.getFloat()

         def encode( c: PacketCodec, v: SFloat, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x66.toByte )	// 'f'
            db.putFloat( v )
         }

         def getEncodedSize( c: PacketCodec, v: SFloat ) = 4
      }

      object Long extends Atom[SLong] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = b.getLong()

         def encode( c: PacketCodec, v: SLong, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x68.toByte )	// 'h'
            db.putLong( v )
         }

         def getEncodedSize( c: PacketCodec, v: SLong ) = 8
      }

      object Double extends Atom[SDouble] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = b.getDouble()

         def encode( c: PacketCodec, v: SDouble, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x64.toByte )	// 'd'
            db.putDouble( v )
         }

      //	def getTypeTag( v: Any ) : Byte  = 0x64	// 'd'
         def getEncodedSize( c: PacketCodec, v: SDouble ) = 8
      }

      object DoubleAsFloat extends Encoder[SDouble] {
         def encode( c: PacketCodec, v: SDouble, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x66.toByte )	// 'f'
            db.putFloat( v.toFloat )
         }
         def getEncodedSize( c: PacketCodec, v: SDouble ) = 4
      }

      object LongAsInt extends Encoder[SLong] {
         def encode( c: PacketCodec, v: SLong, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x69.toByte )	// 'i'
            db.putInt( v.toInt )
         }
         def getEncodedSize( c: PacketCodec, v: SLong ) = 4
      }

      private def decodeString( c: PacketCodec, b: ByteBuffer ) : SString = {
         val pos1	   = b.position
         while( b.get() != 0 ) {}
         val pos2	   = b.position - 1
         b.position( pos1 )
         val len		= pos2 - pos1
         val bytes	= new Array[ SByte ]( len )
         b.get( bytes, 0, len )
         val s       = new String( bytes, c.charsetName )
         val pos3    = (pos2 + 4) & ~3
         if( pos3 > b.limit ) throw new BufferUnderflowException
         b.position( pos3 )
         s
      }

      // parametrized through charsetName
      object String extends Atom[SString] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = decodeString( c, b )

         def encode( c: PacketCodec, v: SString, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x73.toByte )	// 's'
            db.put( v.getBytes( c.charsetName ))  // faster than using Charset or CharsetEncoder
            terminateAndPadToAlign( db )
         }

         def getEncodedSize( c: PacketCodec, v: SString ) = {
            (v.getBytes( c.charsetName ).length + 4) & ~3
         }

         // provide an escaped display
         override def printTextOn( c: PacketCodec, v: SString, stream: PrintStream, nestCount: Int ) {
            OSCPacket.printEscapedStringOn( stream, v )
         }
      }

      // parametrized through charsetName
      object Symbol extends Atom[SSymbol] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = SSymbol( decodeString( c, b ))

         def encode( c: PacketCodec, v: SSymbol, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x53.toByte )	// 'S'
            db.put( v.name.getBytes( c.charsetName ))
            terminateAndPadToAlign( db )
         }

         def getEncodedSize( c: PacketCodec, v: SSymbol ) = {
            (v.name.getBytes( c.charsetName ).length + 4) & ~3
         }

//         // provide an escaped display
//         override def printTextOn( c: PacketCodec, v: Any, stream: PrintStream, nestCount: Int ) {
//            OSCPacket.printEscapedStringOn( stream, v.asInstanceOf[ SSymbol ])
//         }
      }

      object Timetag extends Atom[OSCTimetag] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = OSCTimetag( b.getLong() )

         def encode( c: PacketCodec, v: OSCTimetag, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x74.toByte )	// 't'
            db.putLong( v.raw )
         }

         def getEncodedSize( c: PacketCodec, v: OSCTimetag ) = 8
      }

      /**
       *	Encodes a `java.nio.ByteBuffer` as OSC blob (tag `b`)
       */
      object Blob extends Atom[ByteBuffer] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = {
            val blob = new Array[ SByte ]( b.getInt() )
            b.get( blob )
            skipToAlign( b )
            ByteBuffer.wrap( blob ).asReadOnlyBuffer
         }

         def encode( c: PacketCodec, blob: ByteBuffer, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x62.toByte )	// 'b'
      //		val blob = v.asInstanceOf[ Array[ Byte ]]
//            val blob = v.asInstanceOf[ ByteBuffer ]
            db.putInt( blob.remaining )
            val pos = blob.position
            db.put( blob )
            blob.position( pos )
            padToAlign( db )
         }

         def getEncodedSize( c: PacketCodec, v: ByteBuffer ) = {
            (v.remaining() + 7) & ~3
         }

         override def printTextOn( c: PacketCodec, v: ByteBuffer, stream: PrintStream, nestCount: Int ) {
            stream.print( "DATA[" + v.remaining + "]" )
         }
      }

      /**
       * Encodes an `Packet` as OSC blob (tag `b`)
       */
      object Packet extends Encoder[OSCPacket] {
         def encode( c: PacketCodec, v: OSCPacket, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x62.toByte )	// 'b'
            val pos  = db.position
            val pos2 = pos + 4
//            db.position( pos2 )
            db.putInt( 0 ) // dummy to skip to data; properly throws BufferOverflowException
            v.encode( c, db )
            db.putInt( pos, db.position - pos2 )
         }
         def getEncodedSize( c: PacketCodec, v: OSCPacket ) = {
            v.getEncodedSize( c ) + 4
         }
         override def printTextOn( c: PacketCodec, v: OSCPacket, stream: PrintStream, nestCount: Int ) {
            stream.println()
            v.printTextOn( c, stream, nestCount + 1 )
         }
      }

      object Boolean extends Atom[SBoolean] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) : SBoolean = {
            if( typeTag == 0x54 ) {
               true
            } else if( typeTag == 0x46 ) {
               false
            } else {
               errUnsupported( typeTag.toChar.toString )
            }
         }

         def encode( c: PacketCodec, v: SBoolean, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( if( v ) 0x54.toByte else 0x46.toByte )  // 'T' and 'F'
         }

         def getEncodedSize( c: PacketCodec, v: SBoolean ) = 0
      }

      object BooleanAsInt extends Encoder[SBoolean] {
         def encode( c: PacketCodec, v: SBoolean, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x69.toByte )	// 'i'
            db.putInt( if( v ) 1 else 0 )
         }
         def getEncodedSize( c: PacketCodec, v: SBoolean ) = 4
      }

      object None extends Atom[SNone.type] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) = SNone

         def encode( c: PacketCodec, v: SNone.type, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x4E.toByte )   // 'N'
         }

         def getEncodedSize( c: PacketCodec, v: SNone.type ) = 0
      }

      object Impulse extends Atom[Unit] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) {} // : Unit = ()

         def encode( c: PacketCodec, v: Unit, tb: ByteBuffer, db: ByteBuffer ) {
            tb.put( 0x49.toByte )   // 'I'
         }

         def getEncodedSize( c: PacketCodec, v: Unit ) = 0
      }

      /**
       * Throws exceptions when called
       */
      object Unsupported extends Atom[Any] {
         def decode( c: PacketCodec, typeTag: SByte, b: ByteBuffer ) : Any = errUnsupported( typeTag.toChar.toString )

         def encode( c: PacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
            errUnsupported( v.getClass.getName )
         }

         def getEncodedSize( c: PacketCodec, v: Any ) =
            errUnsupported( v.getClass.getName )

         override def printTextOn( c: PacketCodec, v: Any, stream: PrintStream, nestCount: Int ) {
            stream.print( '\u26A1' )
            stream.print( v.toString )
         }
      }
   }
   trait Atom[ @specialized A ] extends Atom.Encoder[ A ] with Atom.Decoder[ A ]
}

sealed trait Packet {
	def name: String
	
	@throws( classOf[ PacketCodec.Exception ])
	def encode( c: PacketCodec, b: ByteBuffer ) : Unit
	
	def getEncodedSize( c: PacketCodec ) : Int
	private[osc] def printTextOn( c: PacketCodec, stream: PrintStream, nestCount: Int )
}

// they need to be in the same file due to the sealed restriction...

object Bundle {
  /**
   *  This is the initial string
   *  of an OSC bundle datagram
   */
  private[osc] val TAG   = "#bundle"
  private[osc] val TAGB  = "#bundle\0".getBytes

  /**
   *  The special timetag value
   *  to indicate that the bundle be
   *  processed as soon as possible
   */
  val Now   = 1

  private val SECONDS_FROM_1900_TO_1970 = 2208988800L

   /**
    * Creates a bundle with timetag given by
    * a system clock value in milliseconds since
    * jan 1 1970, as returned by System.currentTimeMillis
    */
   def millis( abs: Long, packets: Packet* ) : Bundle =
	   new Bundle( millisToTimetag( abs ), packets: _* )

   /**
    * Creates a bundle with timetag given by
    * a relative value in seconds, as required
    * for example for scsynth offline rendering
    */
   def secs( delta: Double, packets: Packet* ) : Bundle =
	   new Bundle( secsToTimetag( delta ), packets: _* )

   /**
    * Creates a bundle with special timetag 'now'
    */
   def apply( packets: Packet* ) : Bundle = new Bundle( Now, packets: _* )

//   /**
//    * Creates a bundle with raw formatted timetag
//    */
//   def apply( timetag: Long, packets: Packet* ) : Bundle = new Bundle( timetag, packets: _* )

   /**
    * Converts a time value from the system clock value in milliseconds since
    * jan 1 1970, as returned by System.currentTimeMillis, into a raw timetag.
    */
   def millisToTimetag( abs: Long ) : Long = {
      val secsSince1900    = abs / 1000 + SECONDS_FROM_1900_TO_1970
      val secsFractional	= (((abs % 1000) << 32) + 500) / 1000
      (secsSince1900 << 32) | secsFractional
   }

   /**
    * Converts a relative time value in seconds, as required
    * for example for scsynth offline rendering, into a raw timetag.
    */
   def secsToTimetag( delta: Double ) : Long =
      (delta.toLong << 32) + ((delta % 1.0) * 0x100000000L + 0.5).toLong

   /**
    * Converts a raw timetag into a time value from the system clock value in milliseconds since
    * jan 1 1970, corresponding to what is returned by System.currentTimeMillis.
    */
   def timetagToMillis( timetag: Long ) : Long = {
      val m1 = ((timetag & 0xFFFFFFFFL) * 1000) >> 32
      val m2 = (((timetag >> 32) & 0xFFFFFFFFL) - SECONDS_FROM_1900_TO_1970) * 1000
      m1 + m2
   }

   /**
    * Converts a raw timetag into a relative time value in seconds, as required
    * for example for scsynth offline rendering. In general, this will return
    * the amount of seconds since midnight on January 1, 1900, as defined by
    * the OSC standard.
    */
   def timetagToSecs( timetag: Long ) : Double = {
      val frac = (timetag & 0xFFFFFFFFL).toDouble / 0x100000000L
      val secs = (timetag >> 32).toDouble
      secs + frac
   }

//   def unapplySeq( b: Bundle ): Option[ Tuple2[ Long, Seq[ Packet ]]]= Some( b.timetag -> b.packets )

//	@throws( classOf[ IOException ])
//	private[osc] def decode( b: ByteBuffer ) : Bundle = {
//		val totalLimit = b.limit
//		val p			   = new scala.collection.mutable.ListBuffer[ Packet ]
//		val timetag 	= b.getLong
//
//		try {
//			while( b.hasRemaining ) {
//				b.limit( b.getInt + b.position )   // msg size
//				p += decode( b )
//				b.limit( totalLimit )
//			}
//			Bundle( timetag, p: _* )
//		}
//		catch { case e : IllegalArgumentException =>	// throws by b.limit if bundle size is corrupted
//			throw new OSCException( OSCException.DECODE, e.getLocalizedMessage )
//		}
//	}

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

   private def smartTimetagString( timetag: Long ) : String = {
      if( timetag == Now ) "<now>" else {
         val secsSince1900 = (timetag >> 32) & 0xFFFFFFFFL
         if( secsSince1900 > SECONDS_FROM_1900_TO_1970 ) {
            datef.format( timetagToMillis( timetag ))
         } else {
            decimf.format( timetagToSecs( timetag ))
         }
      }
   }
}

final case class Bundle( timetag: Long, packets: Packet* )
extends Packet
with LinearSeqLike[ Packet, Bundle ] {
   import Bundle._

	// ---- getting LinearSeqLike to work properly ----

	def newBuilder : Builder[ Packet, Bundle ] = {
		new scala.collection.mutable.ArrayBuffer[ Packet ] mapResult (buf => new Bundle( timetag, packets: _* ))
	}

	override def iterator : Iterator[ Packet ] = packets.iterator
	override def drop( n: Int ) : Bundle = new Bundle( timetag, packets.drop( n ): _* )
   def apply( idx: Int ) = packets( idx )
   def length: Int = packets.length
   def seq: TraversableOnce[ Packet ] = this // need for Scala 2.9.0

	// ---- Packet implementation ----
	def name: String = Bundle.TAG

	@throws( classOf[ Exception ])
	def encode( c: PacketCodec, b: ByteBuffer ) { c.encodeBundle( this, b )}

	def getEncodedSize( c: PacketCodec ) : Int = c.getEncodedBundleSize( this )

	private[osc] def printTextOn( c: PacketCodec, stream: PrintStream, nestCount: Int ) {
		stream.print( "  " * nestCount )
		stream.print( "[ #bundle, " + smartTimetagString( timetag ))
		val ncInc = nestCount + 1
		for( v <- packets ) {
			stream.println( ',' )
			v.printTextOn( c, stream, ncInc )
		}
		if( nestCount == 0 ) stream.println( " ]" ) else stream.print( " ]" )
	}

   override def toString() = "Bundle(" + smartTimetagString( timetag ) + packets.mkString( ", ", ", ", ")" )
//   override def hashCode = timetag.hashCode * 41 + packets.hashCode
//   override def equals( other: Any ) = other match {
//      case that: Bundle => (that isComparable this) && this.timetag == that.timetag && this.packets == that.packets
//      case _ => false
//   }
//   protected def isComparable( other: Any ) = other.isInstanceOf[ Bundle ]
}

// ------------------------------

object Message {
   def apply( name: String, args: Any* ) = new Message( name, args: _* )
   def unapplySeq( m: Message ): Option[ (String, Seq[ Any ])]= Some( m.name -> m.args )
}

class Message( val name: String, val args: Any* )
extends Packet
with LinearSeqLike[ Any, Message ] {
   import Packet._
   
	// ---- getting LinearSeqLike to work properly ----

	def newBuilder : Builder[ Any, Message ] = {
		new scala.collection.mutable.ArrayBuffer[ Any ] mapResult (buf => new Message( name, buf: _* ))
	}

	override def iterator : Iterator[ Any ] = args.iterator
	override def drop( n: Int ) : Message = new Message( name, args.drop( n ): _* )
   def apply( idx: Int ) = args( idx )
   def length: Int = args.length
   def seq: TraversableOnce[ Any ] = this // need for Scala 2.9.0

	def encode( c: PacketCodec, b: ByteBuffer ) { c.encodeMessage( this, b )}
	def getEncodedSize( c: PacketCodec ) : Int = c.getEncodedMessageSize( this )

   // recreate stuff we lost when removing case modifier
   override def toString() = args.mkString( "Message(" + name + ", ", ", ", ")" )
   override def hashCode() = name.hashCode * 41 + args.hashCode
   override def equals( other: Any ) = other match {
      case that: Message => (that isComparable this) && this.name == that.name && this.args == that.args
      case _ => false
   }
   protected def isComparable( other: Any ) = other.isInstanceOf[ Message ]

	// ---- Packet implementation ----

	private[osc] def printTextOn( c: PacketCodec, stream: PrintStream, nestCount: Int ) {
		stream.print( "  " * nestCount )
		stream.print( "[ " )
		printEscapedStringOn( stream, name )
		for( v <- args ) {
			stream.print( ", " )
			// XXX eventually encoder and decoder should be strictly separated,
			// and hence we would integrate the printing of the incoming messages
			// directly into the decoder!
//			c.atomEncoders( v.asInstanceOf[ AnyRef ].getClass ).printTextOn( c, stream, nestCount, v )

//			c.atomEncoders( v ).printTextOn( c, stream, nestCount, v )
         c.printAtom( v, stream, nestCount )
		}
		if( nestCount == 0 ) stream.println( " ]" ) else stream.print( " ]" )
	}
}