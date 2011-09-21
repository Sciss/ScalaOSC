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
	val default: PacketCodec = Builder().build
  	
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
      /**
       * Creates a new codec builder, initialized
       * to strict OSC 1_0 spec.
       */
      def apply() : Builder = new BuilderImpl
   }
   sealed trait Builder {
      def build: PacketCodec

      var charsetName = "UTF-8"

      /**
       * Resets the builder to strict OSC 1.0 spec,
       * meaning that it accepts only the pairs
       * `Int` - `i`, `Float` - `f`, `String` - `s`,
       * and `ByteBuffer` - `b`.
       */
      def v1_0() : Builder

      /**
       * Resets the builder to strict OSC 1.1 spec,
       * meaning that it accepts only the OSC 1.0 pairs,
       * as well as `Boolean` - `T` and `F`, `None` - `N`,
       * `Unit` - `I`, `Timetag` - `t`.
       *
       * Note that this does not affect the way that
       * packets are encoded with on a TCP stream
       * (see the `TCP` documentation for more information).
       */
      final def v1_1() : Builder = v1_0().booleans().none().impulse().timetags()

      /**
       * Resets the builder to SuperCollider server spec.
       * That is, strict OSC 1.0, plus down-casting of
       * 64-bit numbers to 32-bit, encoding booleans as integers,
       * and packet arguments as blobs, and.
       */
      final def scsynth() : Builder =
         v1_0().doubleToSinglePrecision().booleansAsInts().packetsAsBlobs()

      // ---- optional OSC 1.0 types ----

      def doubles() : Builder
      def longs() : Builder
      final def doublePrecision() : Builder = doubles().longs()

      def symbols() : Builder
      // stupidly, like in the case of String, no charset is defined
      // ("an ascii character, sent as 32 bits" -- but in which encoding???)
      // - should this thus be UTF-32? We currently drop support
      // for 'c'. It also hasn't made it into OSC 1.1
//      def chars() : Builder
      def arrays() : Builder

      // ---- OSC 1.1 types ----

      def booleans() : Builder
      def none() : Builder
      def impulse() : Builder
      def timetags() : Builder

      // ---- SuperCollider types ----
      def doublesAsFloats() : Builder
      def longsAsInts() : Builder
      final def doubleToSinglePrecision() : Builder = doublesAsFloats().longsAsInts()
      def booleansAsInts() : Builder
      def packetsAsBlobs() : Builder
   }

   implicit def build( b: Builder ) : PacketCodec = b.build

   private final class BuilderImpl extends Builder {
      var useDoubles       = false
      var useLongs         = false
      var doubleToFloat    = false
      var longToInt        = false
      var useSymbols       = false
//      var useChars         = false
      var useArrays        = false
      var useBooleans      = false
      var booleanToInt     = false
      var useNone          = false
      var useImpulse       = false
      var useTimetags      = false
      var usePackets       = false

      def v1_0() = {
         useDoubles        = false
         useLongs          = false
         doubleToFloat     = false
         longToInt         = false
         useSymbols        = false
//         useChars          = false
         useArrays         = false
         useBooleans       = false
         booleanToInt      = false
         useNone           = false
         useImpulse        = false
         useTimetags       = false
         usePackets        = false
         this
      }

      def doubles() = {
         useDoubles        = true
         doubleToFloat     = false
         this
      }

      def longs() = {
         useLongs          = true
         longToInt         = false
         this
      }

      def symbols()  = {
         useSymbols        = true
         this
      }

//      def chars() = {
//         useChars          = true
//         this
//      }

      def arrays() = {
         useArrays         = true
         this
      }

      def booleans() = {
         useBooleans       = true
         booleanToInt      = false
         this
      }

      def none() = {
         useNone           = true
         this
      }

      def impulse() =  {
         useImpulse        = true
         this
      }

      def timetags() =  {
         useTimetags       = true
         this
      }

      // ---- SuperCollider types ----
      def doublesAsFloats() = {
         useDoubles        = true
         doubleToFloat     = true
         this
      }

      def longsAsInts() = {
         useLongs          = true
         longToInt         = true
         this
      }

      def booleansAsInts() = {
         useBooleans       = true
         booleanToInt      = true
         this
      }

      def packetsAsBlobs() = {
         usePackets        = true
         this
      }

      def build: PacketCodec = {
         val customEnc     = Map.empty[ Class[ _ ], Atom[ _ ]]   // XXX
//         val customPrint   = Map(
//            classOf[String]      -> Atom.String,
//            classOf[ByteBuffer]  -> Atom.Blob
//         ) ++ customEnc
         new Impl( customEnc, /* customPrint, */ charsetName, useDoubles, useLongs, doubleToFloat, longToInt,
            useSymbols, /* useChars, */ useArrays, useBooleans, booleanToInt, useNone, useImpulse, useTimetags,
            usePackets )
      }
   }

   private final class Impl( customEnc: Map[ Class[ _ ], Atom[ _ ]],
//                             customPrint: Map[ Class[ _ ], Atom ],
                             val charsetName: String,
                             useDoubles: Boolean,
                             useLongs: Boolean,
                             doubleToFloat: Boolean,
                             longToInt: Boolean,
                             useSymbols: Boolean,
//                           useChars: Boolean,
                             useArrays: Boolean,
                             useBooleans: Boolean,
                             booleanToInt: Boolean,
                             useNone: Boolean,
                             useImpulse: Boolean,
                             useTimetags: Boolean,
                             usePackets: Boolean )
   extends PacketCodec {
      codec =>

      override def toString = "PacketCodec"

//      private var atomDecoders = IntMap.empty[ Atom ]
//
//      // ---- constructor ----
//      // OSC version 1.0 strict type tag support
//      atomDecoders += 0x69 -> Atom.Int
//      atomDecoders += 0x66 -> Atom.Float
//      atomDecoders += 0x73 -> Atom.String
//      atomDecoders += 0x62 -> Atom.Blob

      @throws( classOf[ IOException ])
      def encodeBundle( bndl: Bundle, b: ByteBuffer ) {
         try {
            b.put( Bundle.TAGB ).putLong( bndl.timetag )
            bndl.foreach( p => {
               b.mark()
               b.putInt( 0 )			// calculate size later
               val pos1 = b.position
               p.encode( codec, b )
               val pos2 = b.position
               b.reset()
               b.putInt( pos2 - pos1 ).position( pos2 )
            })
         }
         catch { case e: BufferOverflowException =>
            throw PacketCodec.BufferOverflow( bndl.name, e )
         }
      }

      @inline private def encodeAtom( v: Any, tb: ByteBuffer, db: ByteBuffer ) {
         v match {
            case i: Int =>
               tb.put( 0x69.toByte )	   // 'i'
               db.putInt( i )
            case f: Float =>
               tb.put( 0x66.toByte )	   // 'f'
               db.putFloat( f )
            case s: String =>
               tb.put( 0x73.toByte )	   // 's'
               db.put( s.getBytes( charsetName ))  // faster than using Charset or CharsetEncoder
               terminateAndPadToAlign( db )
            case h: Long if( useLongs ) =>
               if( longToInt ) {
                  tb.put( 0x69.toByte )	// 'i'
                  db.putInt( h.toInt )
               } else {
                  tb.put( 0x68.toByte )	// 'h'
                  db.putLong( h )
               }
            case d: Double if( useDoubles ) =>
               if( doubleToFloat ) {
                  tb.put( 0x66.toByte )	// 'f'
                  db.putFloat( d.toFloat )
               } else {
                  tb.put( 0x64.toByte )	// 'd'
                  db.putDouble( d )
               }
            case b: Boolean if( useBooleans ) =>
               if( booleanToInt ) {
                  tb.put( 0x69.toByte )	// 'i'
                  db.putInt( if( b ) 1 else 0 )
               } else {
                  tb.put( if( b ) 0x54.toByte else 0x46.toByte )       // 'T' and 'F'
               }
//               case c: Char if( useChars ) =>
            case blob: ByteBuffer =>
               tb.put( 0x62.toByte )	   // 'b'
               db.putInt( blob.remaining )
               val pos = blob.position
               try {
                  db.put( blob )
               } finally {
                  blob.position( pos )
               }
               padToAlign( db )
            case p: Packet if( usePackets ) =>
               tb.put( 0x62.toByte )	   // 'b'
               val pos  = db.position
               val pos2 = pos + 4
               db.putInt( 0 ) // dummy to skip to data; properly throws BufferOverflowException
               p.encode( codec, db )
               db.putInt( pos, db.position - pos2 )
            case None if( useNone ) =>
               tb.put( 0x4E.toByte )	   // 'N'
            case () if( useImpulse ) =>
               tb.put( 0x49.toByte )	   // 'I'
            case t: Timetag if( useTimetags ) =>
               tb.put( 0x74.toByte )	   // 't'
               db.putLong( t.raw )
            case s: Symbol if( useSymbols ) =>
               tb.put( 0x53.toByte )	   // 'S'
               db.put( s.name.getBytes( charsetName ))  // faster than using Charset or CharsetEncoder
               terminateAndPadToAlign( db )
            case _ => {
               val r = v.asInstanceOf[ AnyRef ]
//               val c: Class[ _ ] = r.getClass
               customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]].encode( codec, r, tb, db )
            }
         }
      }

      def printAtom( v: Any, stream: PrintStream, nestCount: Int ) {
         v match {
            case i: Int =>
               Atom.Int.printTextOn( codec, i, stream, nestCount )
            case f: Float =>
               Atom.Float.printTextOn( codec, f, stream, nestCount )
            case s: String =>
               Atom.String.printTextOn( codec, s, stream, nestCount )
            case h: Long if( useLongs ) =>
               Atom.Long.printTextOn( codec, h, stream, nestCount )
            case d: Double if( useDoubles ) =>
               Atom.Double.printTextOn( codec, d, stream, nestCount )
            case b: Boolean if( useBooleans ) =>
               Atom.Boolean.printTextOn( codec, b, stream, nestCount )
//               case c: Char if( useChars ) =>
            case blob: ByteBuffer =>
               Atom.Blob.printTextOn( codec, blob, stream, nestCount )
            case p: Packet if( usePackets ) =>
               Atom.Packet.printTextOn( codec, p, stream, nestCount )
            case None if( useNone ) =>
               Atom.None.printTextOn( codec, None, stream, nestCount )
            case () if( useImpulse ) =>
               Atom.Impulse.printTextOn( codec, (), stream, nestCount )
            case t: Timetag if( useTimetags ) =>
               Atom.Timetag.printTextOn( codec, t, stream, nestCount )
            case s: Symbol if( useSymbols ) => Atom.Symbol
            case _ =>
               val r = v.asInstanceOf[ AnyRef ]
               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
               atom.printTextOn( codec, r, stream, nestCount )
         }
//         atom.printTextOn( codec, v, stream, nestCount )
      }

      @inline private def encodedAtomSize( v: Any ) : Int = {
         v match {
            case i: Int => 4
            case f: Float => 4
            case s: String => (s.getBytes( charsetName ).length + 4) & ~3
            case h: Long if( useLongs ) => if( longToInt ) 4 else 8
            case d: Double if( useDoubles ) => if( doubleToFloat ) 4 else 8
            case b: Boolean if( useBooleans ) => if( booleanToInt ) 4 else 0
//          case c: Char if( useChars ) => 4
            case blob: ByteBuffer => (blob.remaining() + 7) & ~3
            case p: Packet if( usePackets ) => p.getEncodedSize( codec ) + 4
            case None if( useNone ) => 0
            case () if( useImpulse ) => 0
            case t: Timetag if( useTimetags ) => 8
            case s: Symbol if( useSymbols ) =>  (s.name.getBytes( charsetName ).length + 4) & ~3
            case _ =>
               val r = v.asInstanceOf[ AnyRef ]
               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
               atom.getEncodedSize( codec, r )
         }
      }

      @throws( classOf[ Exception ])
      def encodeMessage( msg: Message, b: ByteBuffer ) {
         try {
            val numArgs = msg.size
            b.put( msg.name.getBytes )  // this one assumes 7-bit ascii only
            terminateAndPadToAlign( b )
            // it's important to slice at a 4-byte boundary because
            // the position will become 0 and terminateAndPadToAlign
            // will be malfunctioning otherwise
            val tb = b.slice
            tb.put( 0x2C.toByte )		// ',' to announce type string
            val pos = b.position + ((numArgs + 5) & ~3)
            if( pos > b.limit ) throw new BufferOverflowException
            b.position( pos )	// comma + numArgs + zero + align
            msg.foreach { v =>
//               val a = atomEncoders( v )
//               a.encode( this, v, tb, b )
               encodeAtom( v, tb, b )
            }
            terminateAndPadToAlign( tb )
         }
         catch { case e: BufferOverflowException =>
            throw BufferOverflow( msg.name, e )
         }
      }

      def getEncodedMessageSize( msg: Message ) : Int = {
         var result  = ((msg.name.length + 4) & ~3) + ((1+msg.length + 4) & ~3)
         msg.foreach { v =>
//            result += atomEncoders( v ).getEncodedSize( codec, v )
            result += encodedAtomSize( v )
         }
         result
      }

      @throws( classOf[ Exception ])
      protected def decodeMessage( name: String, b: ByteBuffer ) : Message = {
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
//               if( !atomDecoders.contains( typ )) throw UnsupportedAtom( typ.toChar.toString )
               val dec = Atom.Unsupported // atomDecoders.getOrElse( typ, Atom.Unsupported )
//            } catch { // note: IntMap throws RuntimeException, _not_ NoSuchElementException!!!
//               case e => throw UnsupportedAtom( typ.toChar.toString )
//            }
               args( argIdx ) = dec.decode( codec, typ, b )
               argIdx += 1
            }
            Message( name, args: _* )
         }
         catch { case e: BufferUnderflowException =>
            throw BufferOverflow( name, e )
         }
      }

//      def printAtom( value: Any, stream: PrintStream, nestCount: Int ) {
//         atomEncoders( value ).printTextOn( codec, value, stream, nestCount )
//      }
   }
}

trait PacketCodec {
   codec =>

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
      bndl.foldLeft( 16 + (bndl.size << 2) )( (sum, p) => sum + p.getEncodedSize( codec ))
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