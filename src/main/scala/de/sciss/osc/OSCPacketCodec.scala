/*
 *  OSCPacketCodec.scala
 *  (ScalaOSC)
 *
 *  Copyright (c) 2008-2011 Hanns Holger Rutz. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
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
import java.nio.{ BufferOverflowException, BufferUnderflowException, ByteBuffer }

import collection.immutable.{ IntMap }
import OSCPacket._

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
 *	OSC typetags, without the need to subclass <code>OSCPacketCodec</code>.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.36, 18-Feb-09
 *
 *	@since		NetUtil 0.35
 */
object OSCPacketCodec {
	lazy val default = new OSCPacketCodec
  	
	val MODE_READ_DOUBLE			= 0x0001
	val MODE_READ_DOUBLE_AS_FLOAT	= 0x0002
	private val MODE_READ_DOUBLE_MASK		= 0x0003
	val MODE_READ_LONG				= 0x0004
	val MODE_READ_LONG_AS_INTEGER	= 0x0008
	private val MODE_READ_LONG_MASK			= 0x000C
	val MODE_WRITE_DOUBLE			= 0x0010
	val MODE_WRITE_DOUBLE_AS_FLOAT	= 0x0020
	private val MODE_WRITE_DOUBLE_MASK		= 0x0030
	val MODE_WRITE_LONG				= 0x0040
	val MODE_WRITE_LONG_AS_INTEGER	= 0x0080
	private val MODE_WRITE_LONG_MASK		= 0x00C0
	val MODE_READ_SYMBOL_AS_STRING	= 0x0100
	val MODE_WRITE_PACKET_AS_BLOB	= 0x0200

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
	 *	<code>de.sciss.net.OSCPacket</code> as a blob <code>'b'</code>.
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
	 *	and <code>de.sciss.net.OSCPacket</code> is encoded as a blob <code>'b'</code>.
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
}

class OSCPacketCodec( mode: Int = OSCPacketCodec.MODE_FAT_V1, var charsetName: String = "UTF-8" ) {
	
	import OSCPacketCodec._
	
//	private[scalaosc] var atomEncoders	= Map.empty[ Class[_], Atom ]
	private[osc] var atomDecoders	= IntMap.empty[ Atom ]
	
	private[osc] val atomEncoders: Function1[ Any, Atom ] = {
		case x: Int => IntAtom
		case x: Float => FloatAtom
		case x: String => StringAtom
		case x: ByteBuffer => BlobAtom

		// XXX use 64-bit fat types
		// here until encoder list updating is implemented

//		if( (mode & MODE_WRITE_PACKET_AS_BLOB) != 0 ) {
			case x: OSCPacket => PacketAtom
//		}
			
		case x: Long => LongAtom
		case x: Double => DoubleAtom
	}

	// constructor
	{
		// OSC version 1.0 strict type tag support
		atomDecoders += 0x69 -> IntAtom
//		atomEncoders += classOf[ Int ] -> IntAtom
//		atomEncoders += classOf[ java.lang.Integer ] -> IntAtom
		atomDecoders += 0x66 -> FloatAtom
//		atomEncoders += classOf[ Float ] -> FloatAtom
//		atomEncoders += classOf[ java.lang.Float ] -> FloatAtom
		atomDecoders += 0x73 -> StringAtom
//		atomEncoders += classOf[ String ] -> StringAtom
		atomDecoders += 0x62 -> BlobAtom
//		atomEncoders += classOf[ ByteBuffer ] -> BlobAtom
				
//		setStringCharsetCodec( charset )
		setSupportMode( mode )
	}

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
	 *	@see	OSCPacketCodec.Atom
	 */
	def putDecoder( typeTag: Byte, a: Atom ) {
		atomDecoders += (typeTag, a)
	}

// ??? why doesn't that get compiled ???
//	def putEncoder( clazz: Class[_], a: Atom ) {
//		atomEncoders += (clazz, a)
//	}

	/**
	 *  Encodes the contents of this packet
	 *  into the provided <code>ByteBuffer</code>,
	 *	beginning at the buffer's current position. To write the
	 *	encoded packet, you will typically call <code>flip()</code>
	 *	on the buffer, then <code>write()</code> on the channel.
	 *
	 *  @param  b							<code>ByteBuffer</code> pointing right at
	 *										the beginning of the osc packet.
	 *										buffer position will be right after the end
	 *										of the packet when the method returns.
	 *
	 *  @throws IOException					in case some of the
	 *										writing procedures failed.
	 */
//	@throws( classOf[ IOException ])
//	def encode( p: OSCPacket, b: ByteBuffer ) : Unit = p.encode( this, b )

	@throws( classOf[ IOException ])
	private[ osc ] def encodeBundle( bndl: OSCBundle, b: ByteBuffer ) {
		b.put( OSCBundle.TAGB ).putLong( bndl.timetag )
//		bndl.synchronized {
			bndl.foreach( p => {
				b.mark()
				b.putInt( 0 )			// calculate size later
				val pos1 = b.position()
//				encode( p, b )
				p.encode( this, b )
				val pos2 = b.position()
				b.reset()
				b.putInt( pos2 - pos1 ).position( pos2 )			
			})
//		}
	}

		/**
	 *	Encodes the message onto the given <code>ByteBuffer</code>,
	 *	beginning at the buffer's current position. To write the
	 *	encoded message, you will typically call <code>flip()</code>
	 *	on the buffer, then <code>write()</code> on the channel.
	 *
	 *  @param  b		<code>ByteBuffer</code> pointing right at
	 *					the beginning of the osc packet.
	 *					buffer position will be right after the end
	 *					of the message when the method returns.
	 *
	 *  @throws IOException			in case some of the
	 *								writing procedures failed
	 *								(buffer overflow, illegal arguments).
	 */
	@throws( classOf[ IOException ])
	private[ osc ] def encodeMessage( msg: OSCMessage, b: ByteBuffer ) : Unit = {
		val numArgs = msg.length

		b.put( msg.name.getBytes() )  // this one assumes 7-bit ascii only
		terminateAndPadToAlign( b )
		// it's important to slice at a 4-byte boundary because
		// the position will become 0 and terminateAndPadToAlign
		// will be malfunctioning otherwise
		val b2 = b.slice();
		b2.put( 0x2C.toByte );		// ',' to announce type string
		b.position( b.position() + ((numArgs + 5) & ~3) )	// comma + numArgs + zero + align
//		try {
			msg.foreach( v => {
//				val cl = v.asInstanceOf[ AnyRef ].getClass
//				val a = atomEncoders( cl )
				val a = atomEncoders( v )
				a.encode( this, v, b2, b )
			})
//		}
//		catch( NullPointerException e1 ) {
//			throw new OSCException( OSCException.JAVACLASS, o == null ? "null" : cl.getName() );
//		}
		terminateAndPadToAlign( b2 )
	}
	
	/**
	 *	Calculates the byte size of the encoded message
	 *
	 *	@return	the size of the OSC message in bytes
	 *
	 *	@throws IOException	if the message contains invalid arguments
	 */
	private[ osc ] def getEncodedMessageSize( msg: OSCMessage ) : Int = {
		var result  = ((msg.name.length + 4) & ~3) + ((1+msg.length + 4) & ~3)
		msg.foreach( v => {
//			val cl = v.asInstanceOf[ AnyRef ].getClass
//			try {
				result += atomEncoders( v ).getEncodedSize( this, v )
//			}
//			catch( NullPointerException e1 ) {
//				throw new OSCException( OSCException.JAVACLASS, cl.getName() );
//			}
		})
		result
	}

	private[ osc ] def getEncodedBundleSize( bndl: OSCBundle ) : Int = {
		var result  = 16 + (bndl.length << 2) // name, timetag, size of each bundle element
		bndl.foreach( result += _.getEncodedSize( this ))
		result
	}
	
	/**
	 * 	Adjusts the support mode for type tag handling. Usually
	 * 	you specify the mode directly in the instantiation of
	 * 	<code>OSCPacketCodec</code>, but you can change it later
	 * 	using this method.
	 * 
	 *	@param	mode	the new mode to use. A flag field combination
	 *					of <code>MODE_READ_DOUBLE</code> or
	 *					<code>MODE_READ_DOUBLE_AS_FLOAT</code> etc.,
	 *					or a ready made combination such as
	 *					<code>MODE_FAT_V1</code>.
	 *
	 *	@see	#OSCPacketCodec( int )
	 */
	def setSupportMode( mode: Int ) {
		(mode & MODE_READ_DOUBLE_MASK) match {
			case MODE_STRICT_V1 => atomDecoders -= 0x64	// 'd' double
			case MODE_READ_DOUBLE => atomDecoders += 0x64 -> DoubleAtom
			case MODE_READ_DOUBLE_AS_FLOAT => atomDecoders += 0x64 -> DoubleAsFloatAtom
			case _ => throw new IllegalArgumentException( String.valueOf( mode ))
		}
		
		(mode & MODE_READ_LONG_MASK) match {
			case MODE_STRICT_V1 => atomDecoders -= 0x68	// 'h' long
			case MODE_READ_LONG => atomDecoders += 0x68 -> LongAtom
			case MODE_READ_LONG_AS_INTEGER => atomDecoders += 0x68 -> LongAsIntAtom
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
			atomDecoders += 0x53 -> StringAtom	// 'S' symbol
		} else {
			atomDecoders -= 0x53
		}

//		if( (mode & MODE_WRITE_PACKET_AS_BLOB) != 0 ) {
//			atomEncoders += classOf[ OSCBundle ] -> PacketAtom
//			atomEncoders += classOf[ OSCMessage ] -> PacketAtom
////			putEncoder( OSCBundle.class, a );
////			putEncoder( OSCMessage.class, a );
//		} else {
//			atomEncoders -= classOf[ OSCBundle ]
//			atomEncoders -= classOf[ OSCMessage ]
////			putEncoder( OSCBundle.class, null );
////			putEncoder( OSCMessage.class, null );
//		}
	}
	
	/**
	 *  Creates a new packet decoded
	 *  from the ByteBuffer. This method tries
	 *  to read a null terminated string at the
	 *  beginning of the provided buffer. If it
	 *  equals the bundle identifier, the
	 *  <code>decode</code> of <code>OSCBundle</code>
	 *  is called (which may recursively decode
	 *  nested bundles), otherwise the one from
	 *  <code>OSCMessage</code>.
	 *
	 *  @param  b   <code>ByteBuffer</code> pointing right at
	 *				the beginning of the packet. the buffer's
	 *				limited should be set appropriately to
	 *				allow the complete packet to be read. when
	 *				the method returns, the buffer's position
	 *				is right after the end of the packet.
	 *
	 *  @return		new decoded OSC packet
	 *  
	 *  @throws IOException					in case some of the
	 *										reading or decoding procedures failed.
	 *  @throws BufferUnderflowException	in case of a parsing
	 *										error that causes the
	 *										method to read past the buffer limit
	 *  @throws IllegalArgumentException	occurs in some cases of buffer underflow
	 */
	@throws( classOf[ IOException ])
	def decode( b: ByteBuffer ) : OSCPacket = {
		val name = readString( b )
		skipToAlign( b )
        
        if( name == "#bundle" ) {
			decodeBundle( b )
        } else {
        	decodeMessage( name, b )
        }
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
//	def getSize( p: OSCPacket ) : Int = {
//		if( p.isInstanceOf[ OSCBundle ]) {
//			getBundleSize( p.asInstanceOf[ OSCBundle ])
//		} else {
//			getMessageSize( p.asInstanceOf[ OSCMessage ])
//		}
//	}
	
//	@throws( classOf[ IOException ])
//	protected def getBundleSize( bndl: OSCBundle ) : Int = {
//		var result = /* OSCPacketCodec.TAGB.length + 8 */ 16 + (bndl.packets.length << 2) // name, timetag, size of each bundle element
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
//	protected def getMessageSize( msg: OSCMessage ) : Int = msg.encodedSize

	@throws( classOf[ IOException ])
	protected def decodeBundle( b: ByteBuffer ) : OSCBundle = {
		val	totalLimit  = b.limit
		val p			= new scala.collection.mutable.ListBuffer[ OSCPacket ]
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

	/**
	 *  Creates a new message with arguments decoded
	 *  from the ByteBuffer. Usually you call
	 *  <code>decode</code> from the <code>OSCPacket</code> 
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
	@throws( classOf[ IOException ])
	protected def decodeMessage( name: String, b: ByteBuffer ) : OSCMessage = {
		if( b.get != 0x2C ) throw new OSCException( OSCException.DECODE, null )
		val b2		= b.slice	// faster to slice than to reposition all the time!
		val pos1	= b.position
		while( b.get != 0x00 ) ()
		val numArgs	= b.position - pos1 - 1
		val args	= new Array[ Any ]( numArgs )
		skipToAlign( b )
	
		var argIdx = 0
		while( argIdx < numArgs ) {
			val typ = b2.get
			try {
				val dec = atomDecoders( typ )
				args( argIdx ) = dec.decode( this, typ, b )
			}
			catch { case _ => throw new OSCException( OSCException.DECODE, String.valueOf( typ.toChar ))}
//			args( argIdx ) = codec.decodeAtom( typ, b )
			argIdx += 1
		}
		OSCMessage( name, args: _* )
	}
}
