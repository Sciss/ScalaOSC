/*
 * PacketCodec.scala
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

import collection.immutable.{ IntMap, IndexedSeq => IIdxSeq }
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
 */
object PacketCodec {
	val default: PacketCodec = apply().build

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

//   object Builder {
      /**
       * Creates a new codec builder, initialized
       * to strict OSC 1_0 spec.
       */
      def apply() : Builder = new BuilderImpl
//   }

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
       * That is, strict OSC 1.0, plus array support,
       * down-casting of 64-bit numbers to 32-bit, encoding booleans as integers,
       * and packet arguments as blobs, and.
       */
      final def scsynth() : Builder =
         v1_0().arrays().doubleToSinglePrecision().booleansAsInts().packetsAsBlobs()

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

     /**
      * Enables support for the OSC 1.0 extended spec's array tags. An 'array' is
      * written out as a type tag '[' (without associated value), followed by the
      * normal encoding of the array elements, and finally another type tag ']'
      * (without associated value).
      *
      * On the Scala side, we wish to enforce a immutable type, hence
      * `collection.immutable.IndexedSeq` was chosen over `Array` for the decoder,
      * while the encoder accepts any `Traversable`
      */
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
      def encode[ A ]( clazz: Class[ A ], enc: Atom.Encoder[ A ]) : Builder
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

      def encode[ A ]( clazz: Class[ A ], enc: Atom.Encoder[ A ]) = {
         customEnc += clazz -> enc
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

      private def decodeString( b: ByteBuffer ) : String = {
         val pos1	   = b.position
         while( b.get() != 0 ) {}
         val pos2	   = b.position - 1
         b.position( pos1 )
         val len		= pos2 - pos1
         val bytes	= new Array[ Byte ]( len )
         b.get( bytes, 0, len )
         val s       = new String( bytes, charsetName )
         val pos3    = (pos2 + 4) & ~3
         if( pos3 > b.limit ) throw new BufferUnderflowException
         b.position( pos3 )
         s
      }

      private def encodeString( b: ByteBuffer, s: String ) {
         b.put( s.getBytes( charsetName ))  // faster than using Charset or CharsetEncoder
         terminateAndPadToAlign( b )
      }

      @throws( classOf[ IOException ])
      def encodeBundle( bndl: Bundle, b: ByteBuffer ) {
         try {
            b.put( BUNDLE_TAGB ).putLong( bndl.timetag.raw )
            bndl.packets.foreach { p =>
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

      @inline private def encodeAtomType( v: Any, tb: ByteBuffer ) {
         v match {
            case i: Int                         => tb.put( 0x69.toByte )	// 'i'
            case f: Float                       => tb.put( 0x66.toByte )	// 'f'
            case s: String                      => tb.put( 0x73.toByte )	// 's'
            case h: Long if( useLongs )         => tb.put(
               if( longToInt ) 0x69.toByte /* 'i' */ else 0x68.toByte /* 'h' */ )
            case d: Double if( useDoubles )     => tb.put(
               if( doubleToFloat ) 0x66.toByte /* 'f' */ else 0x64.toByte /* 'd' */ )
            case b: Boolean if( useBooleans )   => tb.put(
               if( booleanToInt ) 0x69.toByte /* 'i' */ else {
                  if( b ) 0x54.toByte else 0x46.toByte  // 'T' and 'F'
               })
//          case c: Char if( useChars )           =>
            case blob: ByteBuffer               => tb.put( 0x62.toByte )	// 'b'
            case p: Packet if( usePackets )     => tb.put( 0x62.toByte )	// 'b'
            // be careful to place the Traversable after the Packet case, because
            // the packets extends linear seq!
            case c: Traversable[ _ ] if( useArrays ) =>
               tb.put( 0x5B.toByte )
               c.foreach( encodeAtomType( _, tb ))
               tb.put( 0x5D.toByte )

            case None if( useNone )             => tb.put( 0x4E.toByte )   // 'N'
            case u: Unit if( useImpulse )       => tb.put( 0x49.toByte )   // 'I'
            case t: Timetag if( useTimetags )   => tb.put( 0x74.toByte )	// 't'
            case s: Symbol if( useSymbols )     => tb.put( 0x53.toByte )	// 'S'
            case v: Any                         => {
               val r = v.asInstanceOf[ AnyRef ]
               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
               atom.encodeType( codec, r, tb )
            }
         }
      }

      @inline private def encodeAtomData( v: Any, db: ByteBuffer ) {
         v match {
            case i: Int                           => db.putInt( i )
            case f: Float                         => db.putFloat( f )
            case s: String                        => encodeString( db, s )
            case h: Long if( useLongs )           =>
               if( longToInt ) {
                  db.putInt( h.toInt )
               } else {
                  db.putLong( h )
               }
            case d: Double if( useDoubles )       =>
               if( doubleToFloat ) {
                  db.putFloat( d.toFloat )
               } else {
                  db.putDouble( d )
               }
            case b: Boolean if( useBooleans )     => if( booleanToInt ) { db.putInt( if( b ) 1 else 0 )}
//          case c: Char if( useChars )           =>
            case blob: ByteBuffer                 =>
               db.putInt( blob.remaining )
               val pos = blob.position
               db.put( blob )
               blob.position( pos )
               padToAlign( db )

            case p: Packet if( usePackets )       =>
               val pos  = db.position
               val pos2 = pos + 4
               db.putInt( 0 ) // dummy to skip to data; properly throws BufferOverflowException
               p.encode( this, db )
               db.putInt( pos, db.position - pos2 )

            // be careful to place the Traversable after the Packet case, because
            // the packets extends linear seq!
            case c: Traversable[ _ ] if( useArrays ) => c.foreach( encodeAtomData( _, db ))
            case None if( useNone )               =>
            case u: Unit if( useImpulse )         =>
            case t: Timetag if( useTimetags )     => db.putLong( t.raw )
            case s: Symbol if( useSymbols )       => encodeString( db, s.name )
            case v: Any                           => {
               val r = v.asInstanceOf[ AnyRef ]
               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
               atom.encodeData( codec, r, db )
            }
         }
      }

//      @inline private def encodeAtom( v: Any, tb: ByteBuffer, db: ByteBuffer ) {
//         v match {
//            case i: Int                           => Atom.Int.encode( codec, i, tb, db )
//            case f: Float                         => Atom.Float.encode( codec, f, tb, db )
//            case s: String                        => Atom.String.encode( codec, s, tb, db )
//            case h: Long if( useLongs )           =>
//               (if( longToInt ) Atom.LongAsInt else Atom.Long).encode( codec, h, tb, db )
//            case d: Double if( useDoubles )       =>
//               (if( doubleToFloat ) Atom.DoubleAsFloat else Atom.Double).encode( codec, d, tb, db )
//            case b: Boolean if( useBooleans )     =>
//               (if( booleanToInt ) Atom.BooleanAsInt else Atom.Boolean).encode( codec, b, tb, db )
////          case c: Char if( useChars )           =>
//            case blob: ByteBuffer => Atom.Blob.encode( codec, blob, tb, db )
//            case p: Packet if( usePackets )       => Atom.PacketAsBlob.encode( codec, p, tb, db )
////            case c: IIdxSeq[ _ ] if( useArrays )  => Atom.IndexedSeqAsArray.encode( codec, blobk ,tb, db )
//
//            // be careful to place the Traversable after the Packet case, because
//            // the packets extends linear seq!
//            case c: Traversable[ _ ] if( useArrays ) =>
//               tb.put( 0x5B.toByte )
//               c.foreach( encodeAtom( _, tb, db ))
//               tb.put( 0x5D.toByte )
//
//            case None if( useNone )               => Atom.None.encode( codec, None, tb, db )
//            case u: Unit if( useImpulse )         => Atom.Impulse.encode( codec, u, tb, db )
//            case t: Timetag if( useTimetags )     => Atom.Timetag.encode( codec, t, tb, db )
//            case s: Symbol if( useSymbols )       => Atom.Symbol.encode( codec, s, tb, db )
//            case v: Any                           => {
//               val r = v.asInstanceOf[ AnyRef ]
//               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
//               atom.encode( codec, r, tb, db )
//            }
//         }
//      }

      @inline private def decodeAtomData( tt: Byte, db: ByteBuffer ) : Any = {
         (tt.toInt: @switch) match {
            case 0x69                        => db.getInt()          // 'i'
            case 0x66                        => db.getFloat()        // 'f'
            case 0x73                        => decodeString( db )   // 's'
            case 0x68 if( decodeLongs )      => db.getLong()         // 'h'
            case 0x64 if( decodeDoubles )    => db.getDouble()       // 'd'
            case 0x54 if( decodeBooleans )   => true
            case 0x46 if( decodeBooleans )   => false
            case 0x62                        =>
               val blob = new Array[ Byte ]( db.getInt() )
               db.get( blob )
               skipToAlign( db )
               ByteBuffer.wrap( blob ).asReadOnlyBuffer

//          case p: Packet
            case 0x4E if( useNone )          => None
            case 0x49 if( useImpulse )       => ()
            case 0x74 if( useTimetags )      => new Timetag( db.getLong() )
            case 0x53 if( useSymbols )       => Symbol( decodeString( db ))
            case ti                          =>
               customDec.getOrElse( ti, Atom.Unsupported ).decode( this, tt, db )
         }
      }

//      @inline private def getDecoder( tag: Byte ) : Atom.Decoder[_] = {
//         (tag.toInt: @switch) match {
//            case 0x69 => Atom.Int
//            case 0x66 => Atom.Float
//            case 0x73 => Atom.String
//            case 0x68 if( decodeLongs ) => Atom.Long
//            case 0x64 if( decodeDoubles ) => Atom.Double
//            case 0x54 if( decodeBooleans ) => Atom.Boolean
//            case 0x46 if( decodeBooleans ) => Atom.Boolean
//            case 0x62 => Atom.Blob
////          case p: Packet
//            case 0x4E if( useNone ) => Atom.None
//            case 0x49 if( useImpulse ) => Atom.Impulse
//            case 0x74 if( useTimetags ) => Atom.Timetag
//            case 0x53 if( useSymbols ) => Atom.Symbol
//            case ti => customDec.getOrElse( ti, Atom.Unsupported )
//         }
//      }

      def printAtom( v: Any, stream: PrintStream, nestCount: Int ) {
         v match {
            case i: Int                         => stream.print( i )
            case f: Float                       => stream.print( f )
            case s: String                      => Packet.printEscapedStringOn( stream, s )
            case h: Long if( useLongs )         => stream.print( h )
            case d: Double if( useDoubles )     => stream.print( d )
            case b: Boolean if( useBooleans )   => stream.print( b )
//               case c: Char if( useChars ) =>
            case blob: ByteBuffer               => stream.print( "DATA[" + blob.remaining + "]" )
            case p: Packet if( usePackets )     =>
               stream.println()
               p.printTextOn( this, stream, nestCount + 1 )
            // be careful to place the Traversable after the Packet case, because
            // the packets extends linear seq!
            case c: Traversable[ _ ] if( useArrays ) =>
               stream.print( "[ " )
               var sec = false
               c.foreach { v =>
                  if( sec ) {
                     stream.print( ", " )
                  } else {
                     sec = true
                  }
                  printAtom( v, stream, nestCount )
               }
               stream.print( " ]" )
            case None if( useNone )             => stream.print( None )
            case u: Unit if( useImpulse )       => stream.print( () )
            case t: Timetag if( useTimetags )   => stream.print( t )
            case s: Symbol if( useSymbols )     => stream.print( None )
            case r: AnyRef =>
//               val r = v.asInstanceOf[ AnyRef ]
               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
               atom.printTextOn( codec, r, stream, nestCount )
         }
      }

      @throws( classOf[ Exception ])
      def encodeMessage( msg: Message, b: ByteBuffer ) {
         try {
            val a       = msg.args
//            val numArgs = a.size
            b.put( msg.name.getBytes )  // this one assumes 7-bit ascii only
            terminateAndPadToAlign( b )
            // it's important to slice at a 4-byte boundary because
            // the position will become 0 and terminateAndPadToAlign
            // will be malfunctioning otherwise
            b.put( 0x2C.toByte )		// ',' to announce type string
            a.foreach( encodeAtomType( _, b ))
            terminateAndPadToAlign( b )
            a.foreach( encodeAtomData( _, b ))
         }
         catch { case e: BufferOverflowException =>
            throw BufferOverflow( msg.name, e )
         }
      }

      def encodedMessageSize( msg: Message ) : Int = {
         var tsz     = msg.args.size + 5  // initial comma, one per arg, final zero, and max 3 padding zeroes
         var dsz     = 0

         // is this faster than lazy val?
         var calc: Any => Unit = null
         calc = {
            case i: Int => dsz += 4
            case f: Float => dsz += 4
            case s: String => dsz += (s.getBytes( charsetName ).length + 4) & ~3
            case h: Long if( useLongs ) => dsz += (if( longToInt ) 4 else 8)
            case d: Double if( useDoubles ) => dsz += (if( doubleToFloat ) 4 else 8)
            case b: Boolean if( useBooleans ) => dsz += (if( booleanToInt ) 4 else 0)
//          case c: Char if( useChars ) => 4
            case blob: ByteBuffer => dsz += (blob.remaining() + 7) & ~3
            case p: Packet if( usePackets ) => dsz += p.encodedSize( codec ) + 4
            // be careful to place the Traversable after the Packet case, because
            // the packets extends linear seq!
            case c: Traversable[ _ ] if( useArrays ) =>
               tsz += c.size + 1 // a tag for each element plus terminator
               c.foreach( calc )
            case None if( useNone ) =>
            case u: Unit if( useImpulse ) =>
            case t: Timetag if( useTimetags ) => dsz += 8
            case s: Symbol if( useSymbols ) => dsz += (s.name.getBytes( charsetName ).length + 4) & ~3
            case r: AnyRef =>
//               val r = v.asInstanceOf[ AnyRef ]
               val atom = customEnc.getOrElse( r.getClass, Atom.Unsupported ).asInstanceOf[ Atom[ r.type ]]
               dsz += atom.encodedDataSize( codec, r )
         }

         msg.args.foreach( calc )

         ((msg.name.length() + 4) & ~3) + (tsz & ~3) + dsz
      }

      @throws( classOf[ Exception ])
      /* protected */ def decodeMessage( name: String, db: ByteBuffer ) : Message = {
         try {
            if( db.get() != 0x2C ) throw MalformedPacket( name )
            val tb      = db.slice	// faster to slice than to reposition all the time!
            while( db.get() != 0x00 ) ()
            skipToAlign( db )
            val args = decodeMessageArgs( 0x00, tb, db )
            new Message( name, args: _* )
         }
         catch { case e: BufferUnderflowException =>
            throw BufferOverflow( name, e )
         }
      }

      private def decodeMessageArgs( end: Int, tb: ByteBuffer, db: ByteBuffer ) : IIdxSeq[ Any ] = {
         var tt   = tb.get()
         val b    = IIdxSeq.newBuilder[ Any ]
         while( tt != end ) {
            b += (if( tt == 0x5B && useArrays ) {
               decodeMessageArgs( 0x5D, tb, db )   // nested array
            } else {
               decodeAtomData( tt, db )
            })
            tt = tb.get()
         }
         b.result()
      }
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
   *                      requires line breaks. Indentation should be 2x nestCount
   *                      space characters.
   */
  @throws(classOf[Exception])
  def printAtom(value: Any, stream: PrintStream, nestCount: Int): Unit

  /**
   * Encodes the given bundle
   * into the provided <code>ByteBuffer</code>,
   * beginning at the buffer's current position. To write the
   * encoded packet, you will typically call <code>flip()</code>
   * on the buffer, then <code>write()</code> on the channel.
   *
   * @param  b  <code>ByteBuffer</code> pointing right at
   *            the beginning of the osc packet.
   *            buffer position will be right after the end
   *            of the packet when the method returns.
   */
  @throws(classOf[IOException])
  def encodeBundle(bndl: Bundle, b: ByteBuffer): Unit

  /**
   * Encodes the message onto the given <code>ByteBuffer</code>,
   * beginning at the buffer's current position. To write the
   * encoded message, you will typically call <code>flip()</code>
   * on the buffer, then <code>write()</code> on the channel.
   *
   * @param  b	<code>ByteBuffer</code> pointing right at
   *            the beginning of the osc packet.
   *            buffer position will be right after the end
   *            of the message when the method returns.
   */
  @throws(classOf[Exception])
  def encodeMessage(msg: Message, b: ByteBuffer): Unit

  /**
   * Calculates the byte size of the encoded message
   *
   * @return	the size of the OSC message in bytes
   */
  def encodedMessageSize(msg: Message): Int

  /**
   * Calculates the byte size of the encoded bundle.
   * This method is final. The size is the sum
   * of the bundle name, its timetag and the sizes of
   * each bundle element.
   *
   * For contained messages,
   * `encodedMessageSize` will be called, thus for
   * implementations of `PacketCodec`, it is sufficient
   * to overwrite `encodedMessageSize`.
   */
  final def encodedBundleSize(bndl: Bundle): Int = {
    // overhead: name, timetag
    bndl.packets.foldLeft(16 + (bndl.packets.size << 2))((sum, p) => sum + p.encodedSize(codec))
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
   * to overwrite `decodeMessage`.
   *
   * @param  b   <code>ByteBuffer</code> pointing right at
   *             the beginning of the packet. the buffer's
   *             limited should be set appropriately to
   *             allow the complete packet to be read. when
   *             the method returns, the buffer's position
   *             is right after the end of the packet.
   *
   * @return		new decoded OSC packet
   */
  @throws(classOf[Exception])
  final def decode(b: ByteBuffer): Packet = {
    try {
      val name = readString(b)
      skipToAlign(b)
      if (name == "#bundle") {
        decodeBundle(b)
      } else {
        decodeMessage(name, b)
      }
    } catch {
      case e: BufferUnderflowException => throw PacketCodec.BufferOverflow("decode", e)
    }
  }

  @throws(classOf[Exception])
  final def decodeBundle(b: ByteBuffer): Bundle = {
    try {
      val totalLimit  = b.limit
      val p           = new ListBuffer[Packet]
      val timetag     = b.getLong()

      while (b.hasRemaining) {
        val sz = b.getInt() + b.position // msg size
        if (sz > totalLimit) throw new BufferUnderflowException
        b.limit(sz)
        p += decode(b)
        b.limit(totalLimit)
      }
      new Bundle(new Timetag(timetag), p: _*)
    }
    catch {
      case e: BufferUnderflowException => throw PacketCodec.BufferOverflow("#bundle", e)
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
  @throws(classOf[Exception])
  def decodeMessage(name: String, b: ByteBuffer): Message
}