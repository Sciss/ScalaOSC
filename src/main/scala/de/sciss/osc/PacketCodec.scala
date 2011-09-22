/*
 * PacketCodec.scala
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
import annotation.switch

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

      // ---- custom coders ----
      def encode[ A : Manifest ]( enc: Atom.Encoder[ A ]) : Builder
      def decode[ A ]( tag: Byte, dec: Atom.Decoder[ A ]) : Builder
   }

   implicit def build( b: Builder ) : PacketCodec = b.build

   private final class BuilderImpl extends Builder {
      var customEnc        = Map.empty[ Class[ _ ], Atom.Encoder[ _ ]]
      var customDec        = IntMap.empty[ Atom.Decoder[ _ ]]

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
         customEnc         = Map.empty
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

      def encode[ A ]( enc: Atom.Encoder[ A ])( implicit mf: Manifest[ A ]) = {
         customEnc += mf.erasure -> enc
         this
      }

      def decode[ A ]( tag: Byte, dec: Atom.Decoder[ A ]) = {
         customDec += tag.toInt -> dec
         this
      }

      def build: PacketCodec = {
//         val customPrint   = Map(
//            classOf[String]      -> Atom.String,
//            classOf[ByteBuffer]  -> Atom.Blob
//         ) ++ customEnc
         new Impl( customEnc, customDec, charsetName, useDoubles, useLongs, doubleToFloat, longToInt,
            useSymbols, /* useChars, */ useArrays, useBooleans, booleanToInt, useNone, useImpulse, useTimetags,
            usePackets )
      }
   }

   private val BUNDLE_TAGB  = "#bundle\0".getBytes

   private final class Impl( customEnc: Map[ Class[ _ ], Atom.Encoder[ _ ]],
                             customDec: IntMap[ Atom.Decoder[ _ ]],
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

      private val decodeLongs    = useLongs    && !longToInt
      private val decodeDoubles  = useDoubles  && !doubleToFloat
      private val decodeBooleans = useBooleans && !booleanToInt

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
            b.put( BUNDLE_TAGB ).putLong( bndl.timetag.raw )
            bndl.foreach { p =>
               b.mark()
               b.putInt( 0 )			// calculate size later
               val pos1 = b.position
               p.encode( codec, b )
               val pos2 = b.position
               b.reset()
               b.putInt( pos2 - pos1 ).position( pos2 )
            }
         }
         catch { case e: BufferOverflowException =>
            throw PacketCodec.BufferOverflow( bndl.name, e )
         }
      }

      @inline private def encodeAtom( v: Any, tb: ByteBuffer, db: ByteBuffer ) {
         v match {
            case i: Int => Atom.Int.encode( codec, i, tb, db )
            case f: Float => Atom.Float.encode( codec, f, tb, db )
            case s: String => Atom.String.encode( codec, s, tb, db )
            case h: Long if( useLongs ) =>
               (if( longToInt ) Atom.LongAsInt else Atom.Long).encode( codec, h, tb, db )
            case d: Double if( useDoubles ) =>
               (if( doubleToFloat ) Atom.DoubleAsFloat else Atom.Double).encode( codec, d, tb, db )
            case b: Boolean if( useBooleans ) =>
               (if( booleanToInt ) Atom.BooleanAsInt else Atom.Boolean).encode( codec, b, tb, db )
//               case c: Char if( useChars ) =>
            case blob: ByteBuffer => Atom.Blob.encode( codec, blob, tb, db )
            case p: Packet if( usePackets ) => Atom.PacketAsBlob.encode( codec, p, tb, db )
            case None if( useNone ) => Atom.None.encode( codec, None, tb, db )
            case u: Unit if( useImpulse ) => Atom.Impulse.encode( codec, u, tb, db )
            case t: Timetag if( useTimetags ) => Atom.Timetag.encode( codec, t, tb, db )
            case s: Symbol if( useSymbols ) => Atom.Symbol.encode( codec, s, tb, db )
            case r: AnyRef => {
//               val r = v.asInstanceOf[ AnyRef ]
               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
               atom.encode( codec, r, tb, db )
            }
         }
      }

      @inline private def getDecoder( tag: Byte ) : Atom.Decoder[_] = {
         (tag.toInt: @switch) match {
            case 0x69 => Atom.Int
            case 0x66 => Atom.Float
            case 0x73 => Atom.String
            case 0x68 if( decodeLongs ) => Atom.Long
            case 0x64 if( decodeDoubles ) => Atom.Double
            case 0x54 if( decodeBooleans ) => Atom.Boolean
            case 0x46 if( decodeBooleans ) => Atom.Boolean
            case 0x62 => Atom.Blob
//          case p: Packet
            case 0x4E if( useNone ) => Atom.None
            case 0x49 if( useImpulse ) => Atom.Impulse
            case 0x74 if( useTimetags ) => Atom.Timetag
            case 0x53 if( useSymbols ) => Atom.Symbol
            case ti => customDec.getOrElse( ti, Atom.Unsupported )
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
               Atom.PacketAsBlob.printTextOn( codec, p, stream, nestCount )
            case None if( useNone ) =>
               Atom.None.printTextOn( codec, None, stream, nestCount )
            case u: Unit if( useImpulse ) =>
               Atom.Impulse.printTextOn( codec, u, stream, nestCount )
            case t: Timetag if( useTimetags ) =>
               Atom.Timetag.printTextOn( codec, t, stream, nestCount )
            case s: Symbol if( useSymbols ) => Atom.Symbol
            case r: AnyRef =>
//               val r = v.asInstanceOf[ AnyRef ]
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
            case u: Unit if( useImpulse ) => 0
            case t: Timetag if( useTimetags ) => 8
            case s: Symbol if( useSymbols ) =>  (s.name.getBytes( charsetName ).length + 4) & ~3
            case r: AnyRef =>
//               val r = v.asInstanceOf[ AnyRef ]
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
      /* protected */ def decodeMessage( name: String, b: ByteBuffer ) : Message = {
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
////               if( !atomDecoders.contains( typ )) throw UnsupportedAtom( typ.toChar.toString )
//               val dec = Atom.Unsupported // atomDecoders.getOrElse( typ, Atom.Unsupported )
////            } catch { // note: IntMap throws RuntimeException, _not_ NoSuchElementException!!!
////               case e => throw UnsupportedAtom( typ.toChar.toString )
////            }
//               args( argIdx ) = dec.decode( codec, typ, b )
               args( argIdx ) = getDecoder( typ ).decode( codec, typ, b )
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
   /* protected */ final def decodeBundle( b: ByteBuffer ) : Bundle = {
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
         Bundle( Timetag( timetag ), p: _* )
		}
		catch { case e : BufferUnderflowException =>
			throw PacketCodec.BufferOverflow( "#bundle", e )
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
   /* protected */ def decodeMessage( name: String, b: ByteBuffer ) : Message
}