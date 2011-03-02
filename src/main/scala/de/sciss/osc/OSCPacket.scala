/*
 *  OSCPacket.scala
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

import java.io.{ IOException, PrintStream }
import java.nio.{ BufferOverflowException, BufferUnderflowException, ByteBuffer }
import collection.LinearSeqLike
import collection.mutable.Builder
import java.text.{NumberFormat, SimpleDateFormat, DecimalFormat}
import java.util.Locale
object OSCPacket {
	private val HEX				= "0123456789ABCDEF".getBytes
  	private val PAD				= new Array[ Byte ]( 4 )
 
	/**
	 *	Prints a text version of a packet to a given stream.
	 *	The format is similar to scsynth using dump mode 1.
	 *	Bundles will be printed with each message on a separate
	 *	line and increasing indent.
	 *
	 *	@param	stream	the print stream to use, for example <code>System.out</code>
	 *	@param	p		the packet to print (either a message or bundle)
	 */
	def printTextOn( c: OSCPacketCodec, stream: PrintStream, p: OSCPacket ) {
		p.printTextOn( c, stream, 0 )
	}

	/**
	 *	Prints a hexdump version of a packet to a given stream.
	 *	The format is similar to scsynth using dump mode 2.
	 *	Unlike <code>printTextOn</code> this takes a raw received
	 *	or encoded byte buffer and not a decoded instance
	 *	of <code>OSCPacket</code>.
	 *
	 *	@param	stream	the print stream to use, for example <code>System.out</code>
	 *	@param	b		the byte buffer containing the packet. read position
	 *					should be at the very beginning of the packet, limit
	 *					should be at the end of the packet. this method alters
	 *					the buffer position in a manner that a successive <code>flip()</code>
	 *					will restore the original position and limit.
	 *
	 *	@see	java.nio.Buffer#limit()
	 *	@see	java.nio.Buffer#position()
	 */
	def printHexOn( stream: PrintStream, b: ByteBuffer ) {
		val lim	= b.limit
		val	txt	= new Array[ Byte ]( 74 )

		var j = 0
		var k = 0
		var m = 0  
		var n = 0
		var i = 4
		while( i < 56 ) {
			txt( i ) = 0x20
			i += 1
        }
		txt( 56 ) = 0x7C
		
		stream.println
		i = b.position
		while( i < lim ) {
			j = 0
			txt( j )	= HEX( (i >> 12) & 0xF ); j += 1
			txt( j )	= HEX( (i >> 8) & 0xF ); j += 1
			txt( j )	= HEX( (i >> 4) & 0xF ); j += 1
			txt( j )	= HEX( i & 0xF ); j += 1
			m = 57
			k = 0
			while( (k < 16) && (i < lim) ) {
				j += (if( (k & 7) == 0 ) 2 else 1)
				n			= b.get
				txt( j )	= HEX( (n >> 4) & 0xF ); j += 1
				txt( j )	= HEX( n & 0xF ); j += 1
				txt( m )	= (if( (n > 0x1F) && (n < 0x7F) ) n.toByte else 0x2E); m += 1
				k += 1
				i += 1
			}
			txt( m ) = 0x7C; m += 1
			while( j < 54 ) {
				txt( j ) = 0x20; j += 1
			}
			while( m < 74 ) {
				txt( m ) = 0x20; m += 1
			}
			stream.write( txt, 0, 74 )
			stream.println
        }
		stream.println
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
							Integer.toHexString( ch ).toUpperCase()
			    	}
			    } else {
			    	ch match {
                		case '\b' => "\\b"
                		case '\n' => "\\n"
                		case '\t' => "\\t"
                		case '\f' => "\\f"
                		case '\r' => "\\r"
                		case _ => (if( ch > 0xF) "\\u00" else "\\u000") +
                			Integer.toHexString( ch ).toUpperCase()
					}
                }
			)
			i += 1
        }
		stream.print( '\"' )
	}

//	private def printTextOn( stream: PrintStream, p: OSCPacket, nestCount: Int ) {
//		OSCMessage	msg;
//		OSCBundle	bndl;
//		Object		o;
//		
//		if( p instanceof OSCMessage ) {
//			msg = (OSCMessage) p;
//			for( int i = 0; i < nestCount; i++ ) stream.print( "  " );
//			stream.print( "[ \"" + msg.getName() + "\"" );
//			for( int i = 0; i < msg.getArgCount(); i++ ) {
//				o = msg.getArg( i );
//				if( o instanceof Number ) {
//					if( o instanceof Float || o instanceof Double ) {
//						stream.print( ", " + ((Number) o).floatValue() );
//					} else {
//						stream.print( ", " + ((Number) o).longValue() );
//					}
//				} else if( o instanceof OSCPacket ) {
//					stream.println( "," );
//					printTextOn( stream, (OSCPacket) o, nestCount + 1 );
//				} else if( o instanceof byte[] ) {
//					stream.print( ", DATA[" + ((byte[]) o).length + "]" );
//				} else {
//					stream.print( ", \"" + o.toString()+"\"" );
//				}
//			}
//			stream.print( " ]" );
//		} else {
//			bndl = (OSCBundle) p;
//			for( int i = 0; i < nestCount; i++ ) stream.print( "  " );
//			stream.print( "[ \"#bundle\"" );
//			for( int i = 0; i < bndl.getPacketCount(); i++ ) {
//				stream.println( "," );
//				OSCPacket.printTextOn( stream, bndl.getPacket( i ), nestCount + 1 );
//			}
//			for( int i = 0; i < nestCount; i++ ) stream.print( "  " );
//			stream.print( "]" );
//		}
//	
//		if( nestCount == 0 ) stream.println();
//	}
 
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

//	@throws( classOf[ IOException ])
//	def decode( b: ByteBuffer ) : OSCPacket = {
//		val name = readString( b )
//		skipToAlign( b )
//        
//        if( name.equals( "#bundle" )) {
//			OSCBundle.decode( b )
//        } else {
//        	OSCMessage.decode( name, b )
//        }
//	}
}

sealed trait OSCPacket {
	def name: String
	
	@throws( classOf[ OSCException ])
	def encode( c: OSCPacketCodec, b: ByteBuffer ) : Unit
	
	def getEncodedSize( c: OSCPacketCodec ) : Int
	private[osc] def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int )
}

// they need to be in the same file due to the sealed restriction...

object OSCBundle {
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
  val NOW   = 1

  private val SECONDS_FROM_1900_TO_1970 = 2208988800L

   /**
    * Creates a bundle with timetag given by
    * a system clock value in milliseconds since
    * jan 1 1970, as returned by System.currentTimeMillis
    */
   def millis( abs: Long, packets: OSCPacket* ) : OSCBundle =
	   new OSCBundle( millisToTimetag( abs ), packets: _* )

   /**
    * Creates a bundle with timetag given by
    * a relative value in seconds, as required
    * for example for scsynth offline rendering
    */
   def secs( delta: Double, packets: OSCPacket* ) : OSCBundle =
	   new OSCBundle( secsToTimetag( delta ), packets: _* )

   /**
    * Creates a bundle with special timetag 'now'
    */
   def apply( packets: OSCPacket* ) : OSCBundle = new OSCBundle( NOW, packets: _* )

//   /**
//    * Creates a bundle with raw formatted timetag
//    */
//   def apply( timetag: Long, packets: OSCPacket* ) : OSCBundle = new OSCBundle( timetag, packets: _* )

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

//   def unapplySeq( b: OSCBundle ): Option[ Tuple2[ Long, Seq[ OSCPacket ]]]= Some( b.timetag -> b.packets )

	@throws( classOf[ IOException ])
	private[osc] def decode( b: ByteBuffer ) : OSCBundle = {
		val totalLimit = b.limit
		val p			   = new scala.collection.mutable.ListBuffer[ OSCPacket ]
		val timetag 	= b.getLong

		try {
			while( b.hasRemaining ) {
				b.limit( b.getInt + b.position )   // msg size
				p += decode( b )
				b.limit( totalLimit )
			}
			OSCBundle( timetag, p: _* )
		}
		catch { case e : IllegalArgumentException =>	// throws by b.limit if bundle size is corrupted
			throw new OSCException( OSCException.DECODE, e.getLocalizedMessage )
		}
	}

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
      if( timetag == NOW ) "<now>" else {
         val secsSince1900 = (timetag >> 32) & 0xFFFFFFFFL
         if( secsSince1900 > SECONDS_FROM_1900_TO_1970 ) {
            datef.format( timetagToMillis( timetag ))
         } else {
            decimf.format( timetagToSecs( timetag ))
         }
      }
   }
}

final case class OSCBundle( timetag: Long, packets: OSCPacket* )
extends OSCPacket
with LinearSeqLike[ OSCPacket, OSCBundle ] {
   import OSCBundle._

	// ---- getting LinearSeqLike to work properly ----

	def newBuilder : Builder[ OSCPacket, OSCBundle ] = {
		new scala.collection.mutable.ArrayBuffer[ OSCPacket ] mapResult (buf => new OSCBundle( timetag, packets: _* ))
	}

	override def iterator : Iterator[ OSCPacket ] = packets.iterator
	override def drop( n: Int ) : OSCBundle = new OSCBundle( timetag, packets.drop( n ): _* )
   def apply( idx: Int ) = packets( idx )
   def length: Int = packets.length

	// ---- OSCPacket implementation ----
	def name: String = OSCBundle.TAG

	@throws( classOf[ OSCException ])
	def encode( c: OSCPacketCodec, b: ByteBuffer ) : Unit = c.encodeBundle( this, b )

	def getEncodedSize( c: OSCPacketCodec ) : Int = c.getEncodedBundleSize( this )

	private[osc] def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int ) {
		stream.print( "  " * nestCount )
		stream.print( "[ #bundle, " + smartTimetagString( timetag ))
		val ncInc = nestCount + 1
		for( v <- packets ) {
			stream.println( ',' )
			v.printTextOn( c, stream, ncInc )
		}
		if( nestCount == 0 ) stream.println( " ]" ) else stream.print( " ]" )
	}

   override def toString = "OSCBundle(" + smartTimetagString( timetag ) + packets.mkString( ", ", ", ", ")" )
//   override def hashCode = timetag.hashCode * 41 + packets.hashCode
//   override def equals( other: Any ) = other match {
//      case that: OSCBundle => (that isComparable this) && this.timetag == that.timetag && this.packets == that.packets
//      case _ => false
//   }
//   protected def isComparable( other: Any ) = other.isInstanceOf[ OSCBundle ]
}

// ------------------------------

object OSCMessage {
   def apply( name: String, args: Any* ) = new OSCMessage( name, args: _* )
//   def unapply( m: OSCMessage ): Option[ OSCMessage ] = Some( m )
   def unapplySeq( m: OSCMessage ): Option[ Tuple2[ String, Seq[ Any ]]]= Some( m.name -> m.args )
}

class OSCMessage( val name: String, val args: Any* )
extends OSCPacket
with LinearSeqLike[ Any, OSCMessage ]
{
   import OSCPacket._
   
	// ---- getting LinearSeqLike to work properly ----

	def newBuilder : Builder[ Any, OSCMessage ] = {
		new scala.collection.mutable.ArrayBuffer[ Any ] mapResult (buf => new OSCMessage( name, buf: _* ))
	}

	override def iterator : Iterator[ Any ] = args.iterator
	override def drop( n: Int ) : OSCMessage = new OSCMessage( name, args.drop( n ): _* )
   def apply( idx: Int ) = args( idx )
   def length: Int = args.length

	def encode( c: OSCPacketCodec, b: ByteBuffer ) : Unit = c.encodeMessage( this, b )
	def getEncodedSize( c: OSCPacketCodec ) : Int = c.getEncodedMessageSize( this )

   // recreate stuff we lost when removing case modifier
   override def toString = args.mkString( "OSCMessage(" + name + ", ", ", ", ")" )
   override def hashCode = name.hashCode * 41 + args.hashCode
   override def equals( other: Any ) = other match {
      case that: OSCMessage => (that isComparable this) && this.name == that.name && this.args == that.args
      case _ => false
   }
   protected def isComparable( other: Any ) = other.isInstanceOf[ OSCMessage ]

	// ---- OSCPacket implementation ----

	private[osc] def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int ) {
		stream.print( "  " * nestCount )
		stream.print( "[ " )
		printEscapedStringOn( stream, name )
		for( v <- args ) {
			stream.print( ", " )
			// XXX eventually encoder and decoder should be strictly separated,
			// and hence we would integrate the printing of the incoming messages
			// directly into the decoder!
//			c.atomEncoders( v.asInstanceOf[ AnyRef ].getClass ).printTextOn( c, stream, nestCount, v )
			c.atomEncoders( v ).printTextOn( c, stream, nestCount, v )
		}
		if( nestCount == 0 ) stream.println( " ]" ) else stream.print( " ]" )
	}
}