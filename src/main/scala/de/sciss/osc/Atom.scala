/*
 *  Atom.scala
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

import java.io.PrintStream
import java.nio.ByteBuffer
import OSCPacket._

/**
 *    @version	0.11, 23-Nov-09
 */
abstract class Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any
	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) : Unit
	def getEncodedSize( c: OSCPacketCodec, v: Any ) : Int
	def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int, v: Any ) {
		stream.print( v )
	}
}

object IntAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = b.getInt()
	
	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x69.toByte )	// 'i'
		db.putInt( v.asInstanceOf[ Int ])
	}
	
	def getEncodedSize( c: OSCPacketCodec, v: Any ) = 4
}

object FloatAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = b.getFloat()
	
	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x66.toByte )	// 'f'
		db.putFloat( v.asInstanceOf[ Float ] )
	}
	
	def getEncodedSize( c: OSCPacketCodec, v: Any ) = 4
}

object LongAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = b.getLong()

	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x68.toByte )	// 'h'
		db.putLong( v.asInstanceOf[ Long ])
	}

	def getEncodedSize( c: OSCPacketCodec, v: Any ) = 8
}

object DoubleAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = b.getDouble

	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x64.toByte )	// 'd'
		db.putDouble( v.asInstanceOf[ Double ])
	}

//	def getTypeTag( v: Any ) : Byte  = 0x64	// 'd'
	def getEncodedSize( c: OSCPacketCodec, v: Any ) = 8
}

object DoubleAsFloatAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = b.getDouble.toFloat

	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x66.toByte )	// 'f'
		db.putFloat( v.asInstanceOf[ Double ].toFloat )
	}

//	def getTypeTag( v: Any ) : Byte  = 0x66	// 'f'
	def getEncodedSize( c: OSCPacketCodec, v: Any ) = 4
}

object LongAsIntAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = b.getLong.toInt

	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x69.toByte )	// 'i'
		db.putInt( v.asInstanceOf[ Long ].toInt )
	}

	def getEncodedSize( c: OSCPacketCodec, v: Any ) = 4
}

// parametrized through charsetName
object StringAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = {
		val pos1	= b.position()
		while( b.get() != 0 ) {}
		val pos2	= b.position() - 1
		b.position( pos1 )
		val len		= pos2 - pos1
		val bytes	= new Array[ Byte ]( len )
		b.get( bytes, 0, len )
		val s = new String( bytes, c.charsetName )
		b.position( (pos2 + 4) & ~3 )
		s
	}
		
	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x73.toByte )	// 's'
		db.put( v.asInstanceOf[ String ].getBytes( c.charsetName ))  // faster than using Charset or CharsetEncoder
		terminateAndPadToAlign( db )
	}
	
	def getEncodedSize( c: OSCPacketCodec, v: Any ) = {
		(v.asInstanceOf[ String ].getBytes( c.charsetName ).length + 4) & ~3
	}

	// provide an escaped display
	override def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int, v: Any ) {
		OSCPacket.printEscapedStringOn( stream, v.asInstanceOf[ String ])
	}
}

/**
 *	@warning	other than NetUtil this expects a ByteBuffer and not a byte array!
 */
object BlobAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = {
		val blob = new Array[ Byte ]( b.getInt() )
		b.get( blob )
		skipToAlign( b )
		ByteBuffer.wrap( blob ).asReadOnlyBuffer()
	}
	
	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x62.toByte )	// 'b'
//		val blob = v.asInstanceOf[ Array[ Byte ]]
		val blob = v.asInstanceOf[ ByteBuffer ]
		db.putInt( blob.remaining() )
		val pos = blob.position()
		db.put( blob )
		blob.position( pos )
		padToAlign( db )
	}

	def getEncodedSize( c: OSCPacketCodec, v: Any ) = {
		(v.asInstanceOf[ ByteBuffer ].remaining() + 7) & ~3
	}

	override def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int, v: Any ) {
		stream.print( "DATA[" + (v.asInstanceOf[ ByteBuffer ]).remaining + "]" )
	}
}

object PacketAtom extends Atom {
	def decode( c: OSCPacketCodec, typeTag: Byte, b: ByteBuffer ) : Any = {
		throw new java.io.IOException( "Not supported" )
	}
	
	def encode( c: OSCPacketCodec, v: Any, tb: ByteBuffer, db: ByteBuffer ) {
		tb.put( 0x62.toByte )	// 'b'
		val pos = db.position()
		val pos2 = pos + 4
		db.position( pos2 )
		v.asInstanceOf[ OSCPacket ].encode( c, db )
		db.putInt( pos, db.position() - pos2 )
	}

	def getEncodedSize( c: OSCPacketCodec, v: Any ) = {
		v.asInstanceOf[ OSCPacket ].getEncodedSize( c ) + 4
	}

	override def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int, v: Any ) {
		stream.println()
		v.asInstanceOf[ OSCPacket ].printTextOn( c, stream, nestCount + 1 )
	}
}