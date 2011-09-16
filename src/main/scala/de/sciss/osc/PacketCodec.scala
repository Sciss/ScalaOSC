/*
 * OSCPacketCodec.scala
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

import collection.immutable.IntMap
import Packet._
import collection.mutable.ListBuffer
import java.nio.{BufferUnderflowException, BufferOverflowException, ByteBuffer}
import java.io.{PrintStream, IOException}

/**
 *	A packet codec defines how the translation between Java objects
 *	and OSC atoms is accomplished. For example, by default, when
 *	an OSC message is assembled for transmission, the encoder will
 *	translate a<code>java.lang.Integer</code> argument into
 *	a four byte integer with typetag <code>'i'</code>. Or when
 *	a received message is being decoded, finding an atom typetagged
 *	<code>'f'</code>, the decoder will create a <code>java.lang.Float</code>
 *	out of it.
 *	<p>
 *	This example sounds trivial, but the codec is also able to handle
 *	type conversions. For instance, in the strict OSC 1.0 specification,
 *	only 32bit numeric atoms are defined (<code>'i'</code> and <code>'f'</code>).
 *	A codec with mode <code>MODE_STRICT_V1</code> will reject a
 *	<code>java.lang.Double</code> in the encoding process and not be
 *	able to decode a typetag <code>'d'</code>. A codec with mode
 *	<code>MODE_MODEST</code> automatically breaks down everything the 32bit,
 *	so a <code>java.lang.Double</code> gets encoded as 32bit <code>'f'</code>
 *	and a received atom tagged <code>'d'</code> becomes a
 *	<code>java.lang.Float</code>. Other configurations exist.
 *	<p>
 *	Another important function of the codec is to specify the charset encoding
 *	of strings, something that was overseen in the OSC 1.0 spec. By default,
 *	<code>UTF-8</code> is used so all special characters can be safely encoded.
 *	<p>
 *	Last but not least, using the <code>putDecoder</code> and <code>putEncoder</code>
 *	methods, the codec can be extended to support additional Java classes or
 *	OSC typetags, without the need to subclass <code>PacketCodec</code>.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.36, 18-Feb-09
 *
 *	@since		NetUtil 0.35
 */
object PacketCodec {
	lazy val default = new PacketCodecX
  	
	val MODE_READ_DOUBLE			         = 0x0001
	val MODE_READ_DOUBLE_AS_FLOAT	      = 0x0002
	/* private */ val MODE_READ_DOUBLE_MASK	= 0x0003
	val MODE_READ_LONG				      = 0x0004
	val MODE_READ_LONG_AS_INTEGER	      = 0x0008
	/* private */ val MODE_READ_LONG_MASK		= 0x000C
	val MODE_WRITE_DOUBLE			      = 0x0010
	val MODE_WRITE_DOUBLE_AS_FLOAT	   = 0x0020
//	private val MODE_WRITE_DOUBLE_MASK	= 0x0030
	val MODE_WRITE_LONG				      = 0x0040
	val MODE_WRITE_LONG_AS_INTEGER	   = 0x0080
//	private val MODE_WRITE_LONG_MASK		= 0x00C0
	val MODE_READ_SYMBOL_AS_STRING	   = 0x0100
	val MODE_WRITE_PACKET_AS_BLOB	      = 0x0200

	/**
	 *	Support mode: coder only accepts <code>java.lang.Integer</code>,
	 *	<code>java.lang.Float</code>, <code>java.lang.String</code>,
	 *	and <code>byte[]</code>.
	 *	Decoder only accepts <code>'i'</code>, <code>'f'</code>,
	 *	<code>'s'</code>, and <code>'b'</code>. Note that <code>byte[]</code>
	 *	is used to represents blobs (<code>'b'</code>).
	 */
	val MODE_STRICT_V1				= 0x0000
	/**
	 *	Support mode: like <code>MODE_STRICT_V1</code>, but coder additionally
	 *	encodes <code>java.lang.Long</code> as a <code>'i'</code>,
	 *	<code>java.lang.Double</code> as a <code>'f'</code>, and
	 *	<code>de.sciss.net.Packet</code> as a blob <code>'b'</code>.
	 *	The decoder decodes <code>'h'</code> into <code>java.lang.Integer</code>,
	 *	<code>'d'</code> into <code>java.lang.Float</code>, and
	 *	<code>'S'</code> (Symbol) into <code>java.lang.String</code>.
	 */
	val MODE_MODEST					= MODE_READ_DOUBLE_AS_FLOAT | MODE_READ_LONG_AS_INTEGER | MODE_WRITE_DOUBLE_AS_FLOAT | MODE_WRITE_LONG_AS_INTEGER | MODE_READ_SYMBOL_AS_STRING | MODE_WRITE_PACKET_AS_BLOB
	/**
	 *	Support mode: like <code>MODE_MODEST</code>, that is, it will
	 *	downgrade to 32bit in the encoding process, but decoding leaves
	 *	64bit values intact, so <code>'h'</code> becomes <code>java.lang.Long</code>,
	 *	and <code>'d'</code> into <code>java.lang.Double</code>.
	 */
	val MODE_GRACEFUL				= MODE_READ_DOUBLE | MODE_READ_LONG | MODE_WRITE_DOUBLE_AS_FLOAT | MODE_WRITE_LONG_AS_INTEGER | MODE_READ_SYMBOL_AS_STRING | MODE_WRITE_PACKET_AS_BLOB
	/**
	 *	Support mode: like <code>MODE_STRICT_V1</code>, but with additional
	 *	64bit support, that is a mutual mapping between
	 *	<code>'h'</code> &lt;--&gt; <code>java.lang.Long</code>, and
	 *	<code>'d'</code> &lt;--&gt; <code>java.lang.Double</code>.
	 *	Also, <code>'S'</code> (Symbol) is decoded into <code>java.lang.String</code>,
	 *	and <code>de.sciss.net.Packet</code> is encoded as a blob <code>'b'</code>.
	 */
	val MODE_FAT_V1					= MODE_READ_DOUBLE | MODE_READ_LONG | MODE_WRITE_DOUBLE | MODE_WRITE_LONG | MODE_READ_SYMBOL_AS_STRING | MODE_WRITE_PACKET_AS_BLOB
	
	/**
	 * 	Queries the standard codec which is used in all
	 * 	implicit client and server creations. This codec adheres
	 * 	to the <code>MODE_GRACEFUL</code> scheme and uses
	 * 	<code>UTF-8</code> string encoding.
	 * 	<p>
	 * 	Note that although it is not recommended, it is
	 * 	possible to modify the returned codec. That means that
	 * 	upon your application launch, you could query the default
	 * 	codec and switch its behaviour, e.g. change the string
	 * 	charset, so all successive operations with the default
	 * 	codec will be subject to those customizations.
	 * 
	 *	@return	the default codec
	 *	@see	#MODE_GRACEFUL
	 */
//	def getDefaultCodec = defaultCodec

   sealed abstract class Exception( message: String, cause: Throwable )
   extends IOException( message, cause )

   /**
    * An exception thrown during encoding or decoding,
    * indicating that the buffer is too small to
    * encode that packet.
    */
   final case class BufferOverflow( message: String, cause: Throwable )
   extends Exception( message, cause )

   /**
    * An exception thrown during encoding or decoding,
    * indicating that no atom exists to encode or decode
    * a particular type.
    */
   final case class UnsupportedAtom( name: String ) extends Exception( name, null )

   /**
    * An exception thrown during decoding, indicating that
    * the received packet is malformed and does not comply
    * with the OSC standard.
    */
   final case class MalformedPacket( name: String ) extends Exception( name, null )

   object Builder {
      def apply() : Builder = sys.error( "TODO" )
   }
   sealed trait Builder {
      def build: PacketCodec
   }

   implicit def build( b: Builder ) : PacketCodec = b.build
}

trait PacketCodec {
   /**
    * The character set used to encode and decode strings.
    * Unfortunately, this has not been specified in the OSC
    * standard. We recommend to either restrict characters
    * to 7-bit ascii range or to use UTF-8. The default
    * implementation initially uses UTF-8.
    */
   def charsetName: String

   /**
    * Prints a textual representation of the given atom
    * to the given print stream. Implementations should use the
    * atom encoder suitable for the given value.
    *
    * @param   value       the atom to encode
    * @param   stream      the stream to print on
    * @param   nestCount   should only be used if the printing
    *    requires line breaks. Indentation should be 2x nestCount
    *    space characters.
    */
   @throws( classOf[ Exception ])
   def printAtom( value: Any, stream: PrintStream, nestCount: Int ) : Unit

   /**
    * Encodes the given bundle
    * into the provided <code>ByteBuffer</code>,
    *	beginning at the buffer's current position. To write the
    *	encoded packet, you will typically call <code>flip()</code>
    *	on the buffer, then <code>write()</code> on the channel.
    *
    *  @param  b  <code>ByteBuffer</code> pointing right at
    *					the beginning of the osc packet.
    *					buffer position will be right after the end
    *				   of the packet when the method returns.
    */
   @throws( classOf[ IOException ])
   def encodeBundle( bndl: Bundle, b: ByteBuffer ) : Unit

   /**
	 *	Encodes the message onto the given <code>ByteBuffer</code>,
	 *	beginning at the buffer's current position. To write the
	 *	encoded message, you will typically call <code>flip()</code>
	 *	on the buffer, then <code>write()</code> on the channel.
	 *
	 *  @param  b	<code>ByteBuffer</code> pointing right at
	 *					the beginning of the osc packet.
	 *					buffer position will be right after the end
	 *					of the message when the method returns.
	 */
	@throws( classOf[ Exception ])
	def encodeMessage( msg: Message, b: ByteBuffer ) : Unit

   /**
    *	Calculates the byte size of the encoded message
    *
    *	@return	the size of the OSC message in bytes
    */
   def getEncodedMessageSize( msg: Message ) : Int

   /**
    * Calculates the byte size of the encoded bundle.
    * This method is final. The size is the sum
    * of the bundle name, its timetag and the sizes of
    * each bundle element.
    *
    * For contained messages,
    * `getEncodedMessageSize` will be called, thus for
    * implementations of `PacketCodec`, it is sufficient
    * to overwrite `getEncodedMessageSize.
    */
	final def getEncodedBundleSize( bndl: Bundle ) : Int = {
                 // overhead: name, timetag
      bndl.foldLeft( 16 + (bndl.size << 2) )( (sum, p) => sum + p.getEncodedSize( this ))
	}

   /**
    * Creates a new packet decoded
    * from the ByteBuffer. This method tries
    * to read a null terminated string at the
    * beginning of the provided buffer. If it
    * equals the bundle identifier, the
    * <code>decode</code> of <code>Bundle</code>
    * is called (which may recursively decode
    * nested bundles), otherwise the one from
    * <code>Message</code>.
    *
    * This method is final. For messages encountered,
    * `decodeMessage` is called, , thus for
    * implementations of `PacketCodec`, it is sufficient
    * to overwrite `decodeMessage.
    *
    *  @param  b   <code>ByteBuffer</code> pointing right at
    *				the beginning of the packet. the buffer's
    *				limited should be set appropriately to
    *				allow the complete packet to be read. when
    *				the method returns, the buffer's position
    *				is right after the end of the packet.
    *
    *  @return		new decoded OSC packet
    */
   @throws( classOf[ Exception ])
   final def decode( b: ByteBuffer ) : Packet = {
      try {
         val name = readString( b )
         skipToAlign( b )
         if( name eq /* == */ "#bundle" ) {
            decodeBundle( b )
         } else {
            decodeMessage( name, b )
         }
      } catch { case e: BufferUnderflowException =>
         throw PacketCodec.BufferOverflow( "decode", e )
      }
   }

   @throws( classOf[ Exception ])
   protected final def decodeBundle( b: ByteBuffer ) : Bundle = {
      try {
         val totalLimit = b.limit
         val p			   = new ListBuffer[ Packet ]
         val timetag    = b.getLong()

         while( b.hasRemaining ) {
            val sz = b.getInt + b.position // msg size
            if( sz > totalLimit ) throw new BufferUnderflowException
            b.limit( sz )
            p += decode( b )
            b.limit( totalLimit )
         }
         Bundle( timetag, p: _* )
		}
		catch { case e : BufferUnderflowException =>
			throw PacketCodec.BufferOverflow( Bundle.TAG, e )
		}
	}

   /**
    * Decodes a message with a given name and buffer holding
    * its arguments.
    *
    * Implementations should be careful to
    * catch potential instances of `BufferUnderflowException`
    * or other runtime exception such as `IllegalArgumentException`
    * and cast them into instances of `PacketCodec.Exception`,
    * such that the caller can be safe to catch any error by
    * matching against `PacketCodec.Exception`.
    *
    * @param   name  the name of the message which has already
    *                been decoded when this method is called.
    * @param   b     the buffer, positioned at the type tags
    *                list (beginning with `','`).
    */
   @throws( classOf[ Exception ])
   protected def decodeMessage( name: String, b: ByteBuffer ) : Message
}

final class PacketCodecX( mode: Int = PacketCodec.MODE_FAT_V1, var charsetName: String = "UTF-8" )
extends PacketCodec {
   import PacketCodec._

//	private[scalaosc] var atomEncoders	= Map.empty[ Class[_], Atom ]
   private[osc] var atomDecoders	= IntMap.empty[ Atom ]

   private[osc] val atomEncoders: Any => Atom = {
      case x: Int          => Atom.Int
      case x: Float        => Atom.Float
      case x: String       => Atom.String
      case x: ByteBuffer   => Atom.Blob

      // XXX use 64-bit fat types
      // here until encoder list updating is implemented

//		if( (mode & MODE_WRITE_PACKET_AS_BLOB) != 0 ) {
         case x: Packet  => Atom.Packet
//		}

      case x: Long         => Atom.Long
      case x: Double       => Atom.Double
      case x               => throw UnsupportedAtom( x.getClass.getName )
   }

	// ---- constructor ----
	// OSC version 1.0 strict type tag support
   atomDecoders += 0x69 -> Atom.Int
//	atomEncoders += classOf[ Int ] -> IntAtom
//	atomEncoders += classOf[ java.lang.Integer ] -> IntAtom
   atomDecoders += 0x66 -> Atom.Float
//	atomEncoders += classOf[ Float ] -> FloatAtom
//	atomEncoders += classOf[ java.lang.Float ] -> FloatAtom
   atomDecoders += 0x73 -> Atom.String
// atomEncoders += classOf[ String ] -> StringAtom
   atomDecoders += 0x62 -> Atom.Blob
//	atomEncoders += classOf[ ByteBuffer ] -> BlobAtom

//	setStringCharsetCodec( charset )
	setSupportMode( mode )

	/**
	 * 	Registers an atomic decoder with the packet codec. This
	 * 	decoder is called whenever an OSC message with the
	 * 	given typetag is encountered.
	 * 
	 *	@param typeTag	the typetag which is to be decoded with the
	 *					new <code>Atom</code>. <code>typeTag</code>
	 *					must be in the ASCII value range 0 to 127.
	 *	@param a		the decoder to use
	 *
	 *	@see	PacketCodec.Atom
	 */
	def putDecoder( typeTag: Byte, a: Atom ) {
		atomDecoders += (typeTag, a)
	}

// ??? why doesn't that get compiled ???
//	def putEncoder( clazz: Class[_], a: Atom ) {
//		atomEncoders += (clazz, a)
//	}

	@throws( classOf[ IOException ])
	final def encodeBundle( bndl: Bundle, b: ByteBuffer ) {
      try {
   		b.put( Bundle.TAGB ).putLong( bndl.timetag )
			bndl.foreach( p => {
				b.mark()
				b.putInt( 0 )			// calculate size later
				val pos1 = b.position
//				encode( p, b )
				p.encode( this, b )
				val pos2 = b.position
				b.reset()
				b.putInt( pos2 - pos1 ).position( pos2 )			
			})
		}
      catch { case e: BufferOverflowException =>
         throw PacketCodec.BufferOverflow( bndl.name, e )
      }
	}

	@throws( classOf[ Exception ])
	final def encodeMessage( msg: Message, b: ByteBuffer ) {
      try {
         val numArgs = msg.size
         b.put( msg.name.getBytes )  // this one assumes 7-bit ascii only
         terminateAndPadToAlign( b )
         // it's important to slice at a 4-byte boundary because
         // the position will become 0 and terminateAndPadToAlign
         // will be malfunctioning otherwise
         val b2 = b.slice
         b2.put( 0x2C.toByte )		// ',' to announce type string
         val pos = b.position + ((numArgs + 5) & ~3)
         if( pos > b.limit ) throw new BufferOverflowException
         b.position( pos )	// comma + numArgs + zero + align
			msg.foreach { v =>
				val a = atomEncoders( v )
				a.encode( this, v, b2, b )
			}
         terminateAndPadToAlign( b2 )
		}
		catch { case e: BufferOverflowException =>
         throw BufferOverflow( msg.name, e )
		}
	}
	
	final def getEncodedMessageSize( msg: Message ) : Int = {
		var result  = ((msg.name.length + 4) & ~3) + ((1+msg.length + 4) & ~3)
		msg.foreach { v =>
         result += atomEncoders( v ).getEncodedSize( this, v )
		}
		result
	}

	/**
	 * 	Adjusts the support mode for type tag handling. Usually
	 * 	you specify the mode directly in the instantiation of
	 * 	<code>PacketCodec</code>, but you can change it later
	 * 	using this method.
	 * 
	 *	@param	mode	the new mode to use. A flag field combination
	 *					of <code>MODE_READ_DOUBLE</code> or
	 *					<code>MODE_READ_DOUBLE_AS_FLOAT</code> etc.,
	 *					or a ready made combination such as
	 *					<code>MODE_FAT_V1</code>.
	 *
	 *	@see	#PacketCodec( int )
	 */
	def setSupportMode( mode: Int ) {
		(mode & MODE_READ_DOUBLE_MASK) match {
			case MODE_STRICT_V1 => atomDecoders -= 0x64	// 'd' double
			case MODE_READ_DOUBLE => atomDecoders += 0x64 -> Atom.Double
			case MODE_READ_DOUBLE_AS_FLOAT => atomDecoders += 0x64 -> Atom.DoubleAsFloat
			case _ => throw new IllegalArgumentException( String.valueOf( mode ))
		}
		
		(mode & MODE_READ_LONG_MASK) match {
			case MODE_STRICT_V1 => atomDecoders -= 0x68	// 'h' long
			case MODE_READ_LONG => atomDecoders += 0x68 -> Atom.Long
			case MODE_READ_LONG_AS_INTEGER => atomDecoders += 0x68 -> Atom.LongAsInt
			case _ => throw new IllegalArgumentException( String.valueOf( mode ))
		}

//		(mode & MODE_WRITE_DOUBLE_MASK) match {
//			case MODE_STRICT_V1 => {
//				atomEncoders -= classOf[ Double ]
//				atomEncoders -= classOf[ java.lang.Double ]
////				putEncoder( Double.class, null );
//			}
//			case MODE_WRITE_DOUBLE => {
//				atomEncoders += classOf[ Double ] -> DoubleAtom
//				atomEncoders += classOf[ java.lang.Double ] -> DoubleAtom
////				putEncoder( Double.class, new DoubleAtom() );
//			}
//			case MODE_WRITE_DOUBLE_AS_FLOAT => {
//				atomEncoders += classOf[ Double ] -> DoubleAsFloatAtom
//				atomEncoders += classOf[ java.lang.Double ] -> DoubleAsFloatAtom
////				putEncoder( Double.class, new DoubleAsFloatAtom() );
//			}
//			case _ => throw new IllegalArgumentException( String.valueOf( mode ))
//		}
//		
//		(mode & MODE_WRITE_LONG_MASK) match {
//			case MODE_STRICT_V1 => {
//				atomEncoders -= classOf[ Long ]
//				atomEncoders -= classOf[ java.lang.Long ]
////				putEncoder( Long.class, null );
//			}
//			case MODE_WRITE_LONG => {
//				atomEncoders += classOf[ Long ] -> LongAtom
//				atomEncoders += classOf[ java.lang.Long ] -> LongAtom
////				putEncoder( Long.class, new LongAtom() );
//			}
//			case MODE_WRITE_LONG_AS_INTEGER => {
//				atomEncoders += classOf[ Long ] -> LongAsIntAtom
//				atomEncoders += classOf[ java.lang.Long ] -> LongAsIntAtom
////				putEncoder( Long.class, new LongAsIntAtom() );
//			}
//			case _ => throw new IllegalArgumentException( String.valueOf( mode ))
//		}
		
		if( (mode & MODE_READ_SYMBOL_AS_STRING) != 0 ) {
			atomDecoders += 0x53 -> Atom.String // 'S' symbol
		} else {
			atomDecoders -= 0x53
		}

//		if( (mode & MODE_WRITE_PACKET_AS_BLOB) != 0 ) {
//			atomEncoders += classOf[ Bundle ] -> PacketAtom
//			atomEncoders += classOf[ Message ] -> PacketAtom
////			putEncoder( Bundle.class, a );
////			putEncoder( Message.class, a );
//		} else {
//			atomEncoders -= classOf[ Bundle ]
//			atomEncoders -= classOf[ Message ]
////			putEncoder( Bundle.class, null );
////			putEncoder( Message.class, null );
//		}
	}

	/**
	 *  Calculates and returns
	 *  the packet's size in bytes
	 *
	 *  @return the size of the packet in bytes, including the initial
	 *			osc command and aligned to 4-byte boundary. this
	 *			is the amount of bytes written by the <code>encode</code>
	 *			method.
	 *	 
	 *  @throws IOException if an error occurs during the calculation
	 */
//	@throws( classOf[ IOException ])
//	def getSize( p: Packet ) : Int = {
//		if( p.isInstanceOf[ Bundle ]) {
//			getBundleSize( p.asInstanceOf[ Bundle ])
//		} else {
//			getMessageSize( p.asInstanceOf[ Message ])
//		}
//	}
	
//	@throws( classOf[ IOException ])
//	protected def getBundleSize( bndl: Bundle ) : Int = {
//		var result = /* PacketCodec.TAGB.length + 8 */ 16 + (bndl.packets.length << 2) // name, timetag, size of each bundle element
//		for( p <- bndl.packets ) {
//			result += getSize( p )
//		}
//		result
//	}
	
	/**
	 *	Calculates the byte size of the encoded message
	 *
	 *	@return	the size of the OSC message in bytes
	 *
	 *	@throws IOException	if the message contains invalid arguments
	 */
//	@throws( classOf[ IOException ])
//	protected def getMessageSize( msg: Message ) : Int = msg.encodedSize

	/**
	 *  Creates a new message with arguments decoded
	 *  from the ByteBuffer. Usually you call
	 *  <code>decode</code> from the <code>Packet</code>
	 *  superclass which will invoke this method of
	 *  it finds an OSC message.
	 *
	 *  @param  b   ByteBuffer pointing right at
	 *				the beginning of the type
	 *				declaration section of the
	 *				OSC message, i.e. the name
	 *				was skipped before.
	 *
	 *  @return		new OSC message representing
	 *				the received message described
	 *				by the ByteBuffer.
	 *  
	 *  @throws IOException					in case some of the
	 *										reading or decoding procedures failed.
	 *  @throws BufferUnderflowException	in case of a parsing
	 *										error that causes the
	 *										method to read past the buffer limit
	 *  @throws IllegalArgumentException	occurs in some cases of buffer underflow
	 */
   @throws( classOf[ Exception ])
   protected final def decodeMessage( name: String, b: ByteBuffer ) : Message = {
      try {
         if( b.get() != 0x2C ) throw MalformedPacket( name )
         val b2      = b.slice	// faster to slice than to reposition all the time!
         val pos1	   = b.position
         while( b.get() != 0x00 ) ()
         val numArgs	= b.position - pos1 - 1
         // note: Array filling is much faster than ListBuffer
         // (with numArgs == 6, the whole thing should be approx. 4x faster)
//         val args	   = new ListBuffer[ Any ]
         val args	   = new Array[ Any ]( numArgs )
         skipToAlign( b )

         var argIdx = 0
         while( argIdx < numArgs ) {
            val typ = b2.get()
            if( !atomDecoders.contains( typ )) throw UnsupportedAtom( typ.toChar.toString )
            val dec = atomDecoders( typ )
//            } catch { // note: IntMap throws RuntimeException, _not_ NoSuchElementException!!!
//               case e => throw UnsupportedAtom( typ.toChar.toString )
//            }
            args( argIdx ) = dec.decode( this, typ, b )
            argIdx += 1
         }
         Message( name, args: _* )
      }
      catch { case e: BufferUnderflowException =>
         throw BufferOverflow( name, e )
      }
   }

   final def printAtom( value: Any, stream: PrintStream, nestCount: Int ) {
      atomEncoders( value ).printTextOn( this, value, stream, nestCount )
   }
}
