/*
 * Packet.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2020 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc

import java.io.PrintStream
import java.nio.{BufferOverflowException, BufferUnderflowException, ByteBuffer}

object Packet {
  private val HEX = "0123456789ABCDEF".getBytes
  private val PAD = new Array[Byte](4)

  /** Prints a text version of a packet to a given stream.
    * The format is similar to scsynth using dump mode 1.
    * Bundles will be printed with each message on a separate
    * line and increasing indent.
    *
    * @param	stream   the print stream to use, for example <code>System.out</code>
    * @param	p        the packet to print (either a message or bundle)
    */
  def printTextOn(p: Packet, c: PacketCodec, stream: PrintStream): Unit = p.printTextOn(c, stream, 0)

  /** Prints a hex-dump version of a packet to a given stream.
    * The format is similar to scsynth using dump mode 2.
    * Unlike <code>printTextOn</code> this takes a raw received
    * or encoded byte buffer and not a decoded instance
    * of <code>Packet</code>.
    *
    * @param	stream	the print stream to use, for example <code>System.out</code>
    * @param	b		    the byte buffer containing the packet. the current position
    *                 is saved, and the printing is performed from position 0 to
    *                 the limit of the buffer. the previous position is restored.
    *
    * @see	java.nio.Buffer#limit()
    * @see	java.nio.Buffer#position()
    */
  def printHexOn(b: ByteBuffer, stream: PrintStream): Unit = {
    val pos0: Int = b.position
    try {
      val lim: Int = b.limit
      val txt = new Array[Byte](74)

      var j = 0
      var k = 0
      var m = 0
      var n = 0
      var i = 4
      while (i < 56) {
        txt(i) = 0x20.toByte
        i += 1
      }
      txt(56) = 0x7C.toByte

      stream.println()
      b.position(0)
      i = 0; while (i < lim) {
        j = 0
        txt(j) = HEX((i >> 12) & 0xF); j += 1
        txt(j) = HEX((i >>  8) & 0xF); j += 1
        txt(j) = HEX((i >>  4) & 0xF); j += 1
        txt(j) = HEX( i        & 0xF); j += 1
        m = 57
        k = 0
        while ((k < 16) && (i < lim)) {
          j += (if ((k & 7) == 0) 2 else 1)
          n = b.get()
          txt(j) = HEX((n >> 4) & 0xF); j += 1
          txt(j) = HEX( n       & 0xF); j += 1
          txt(m) = if ((n > 0x1F) && (n < 0x7F)) n.toByte else 0x2E.toByte; m += 1
          k += 1
          i += 1
        }
        txt(m) = 0x7C.toByte; m += 1
        while (j < 54) {
          txt(j) = 0x20.toByte; j += 1
        }
        while (m < 74) {
          txt(m) = 0x20.toByte; m += 1
        }
        stream.write(txt, 0, 74)
        stream.println()
      }
      stream.println()
    } finally {
      b.position(pos0)
    }
  }

  def printEscapedStringOn(stream: PrintStream, str: String): Unit = {
    stream.print('\"')
    val numChars = str.length
    var i = 0
    while (i < numChars) {
      val ch = str.charAt(i)
      stream.print(
        if (ch >= 32) {
          if (ch < 0x80) {
            if (ch == '"') "\\\"" else if (ch == '\\') "\\\\" else ch
          } else {
            (if (ch < 0x100) "\\u00" else if (ch < 0x1000) "\\u0" else "\\u") +
              Integer.toHexString(ch).toUpperCase
          }
        } else {
          ch match {
            case '\b' => "\\b"
            case '\n' => "\\n"
            case '\t' => "\\t"
            case '\f' => "\\f"
            case '\r' => "\\r"
            case _ => (if (ch > 0xF) "\\u00" else "\\u000") +
              Integer.toHexString(ch).toUpperCase
          }
        }
      )
      i += 1
    }
    stream.print('\"')
  }

  /** Reads a null terminated string from
    * the current buffer position
    *
    * @param  b   buffer to read from. position and limit must be
    *             set appropriately. new position will be right after
    *             the terminating zero byte when the method returns
    *
    * @throws BufferUnderflowException	in case the string exceeds
    *                                   the provided buffer limit
    */
  @throws(classOf[BufferUnderflowException])
  def readString(b: ByteBuffer): String = {
    val pos: Int = b.position
    while (b.get != 0) ()
    val len = (b.position: Int) - pos
    val bytes = new Array[Byte](len)
    b.position(pos)
    b.get(bytes)
    new String(bytes, 0, len - 1)
  }

  /** Adds as many zero padding bytes as necessary to
    * stop on a 4 byte alignment. if the buffer position
    * is already on a 4 byte alignment when calling this
    * function, another 4 zero padding bytes are added.
    * buffer position will be on the new aligned boundary
    * when return from this method
    *
    * @param  b   the buffer to pad
    *
    * @throws BufferOverflowException		in case the padding exceeds
    *                                    the provided buffer limit
    */
  @throws(classOf[BufferOverflowException])
  def terminateAndPadToAlign(b: ByteBuffer): Unit =
    b.put(PAD, 0, 4 - ((b.position: Int) & 0x03))

  /** Adds as many zero padding bytes as necessary to
    * stop on a 4 byte alignment. if the buffer position
    * is already on a 4 byte alignment when calling this
    * function, this method does nothing.
    *
    * @param  b   the buffer to align
    *
    * @throws BufferOverflowException		in case the padding exceeds
    *                                    the provided buffer limit
    */
  @throws(classOf[BufferOverflowException])
  def padToAlign(b: ByteBuffer): Unit =
    b.put(PAD, 0, -(b.position: Int) & 0x03) // nearest 4-align

  /** Advances in the buffer as long there
    * are non-zero bytes, then advance to a
    * four byte alignment.
    *
    * @param  b   the buffer to advance
    *
    * @throws BufferUnderflowException	in case the reads exceed
    *                                   the provided buffer limit
    */
  @throws(classOf[BufferUnderflowException])
  def skipToValues(b: ByteBuffer): Unit = {
    while (b.get != 0x00) ()
    val newPos = ((b.position: Int) + 3) & ~3
    if (newPos > (b.limit: Int)) throw new BufferUnderflowException
    b.position(newPos)
  }

  /** Advances the current buffer position
    * to an integer of four bytes. The position
    * is not altered if it is already
    * aligned to a four byte boundary.
    *
    * @param  b   the buffer to advance
    *
    * @throws BufferUnderflowException	in case the skipping exceeds
    *                                   the provided buffer limit
    */
  @throws(classOf[BufferUnderflowException])
  def skipToAlign(b: ByteBuffer): Unit = {
    val newPos = ((b.position: Int) + 3) & ~3
    if (newPos > (b.limit: Int)) throw new BufferUnderflowException
    b.position(newPos)
  }

  object Atom {
    import scala.{Byte => SByte}

    private def errUnsupported(text: String): Nothing = throw PacketCodec.UnsupportedAtom(text)

    trait Encoder[@specialized A] {
      def encodeType(c: PacketCodec, v: A, tb: ByteBuffer): Unit
      // def encode( c: PacketCodec, v: A, tb: ByteBuffer, db: ByteBuffer ) : Unit
      def encodeData(c: PacketCodec, v: A, db: ByteBuffer): Unit
      def encodedDataSize(c: PacketCodec, v: A): Int

      def printTextOn(c: PacketCodec, v: A, stream: PrintStream, nestCount: Int): Unit = stream.print(v)
    }

    trait Decoder[@specialized A] {
      def decode(c: PacketCodec, typeTag: SByte, b: ByteBuffer): A
    }

    /** Throws exceptions when called */
    object Unsupported extends Atom[Any] {
      def decode(c: PacketCodec, typeTag: SByte, b: ByteBuffer): Any =
        errUnsupported(typeTag.toChar.toString)

      private def errUnsupportedData(v: Any): Nothing = errUnsupported(v.asInstanceOf[AnyRef].getClass.getName)

      def encodeType(c: PacketCodec, v: Any, tb: ByteBuffer): Unit = errUnsupportedData(v)
      def encodeData(c: PacketCodec, v: Any, db: ByteBuffer): Unit = errUnsupportedData(v)

      def encodedDataSize(c: PacketCodec, v: Any): Int = errUnsupportedData(v)

      override def printTextOn(c: PacketCodec, v: Any, stream: PrintStream, nestCount: Int): Unit = {
        stream.print('\u26A1')
        stream.print(v.toString)
      }
    }
  }

  trait Atom[@specialized A] extends Atom.Encoder[A] with Atom.Decoder[A]
}

sealed trait Packet {
  def name: String

  @throws(classOf[PacketCodec.Exception])
  def encode(c: PacketCodec, b: ByteBuffer): Unit

  def encodedSize(c: PacketCodec): Int

  private[osc] def printTextOn(c: PacketCodec, stream: PrintStream, nestCount: Int): Unit
}

// they need to be in the same file due to the sealed restriction...

object Bundle {
  /** Creates a bundle with time-tag given by
    * a system clock value in milliseconds since
    * jan 1 1970, as returned by System.currentTimeMillis
    */
  def millis(abs: Long, packets: Packet*): Bundle =
    new Bundle(TimeTag.millis(abs), packets: _*)

  /** Creates a bundle with time-tag given by
    * a relative value in seconds, as required
    * for example for scsynth offline rendering
    */
  def secs(delta: Double, packets: Packet*): Bundle =
    new Bundle(TimeTag.secs(delta), packets: _*)

  /** Creates a bundle with special time-tag 'now' */
  def now(packets: Packet*): Bundle = new Bundle(TimeTag.now, packets: _*)
}

final case class Bundle(timeTag: TimeTag, packets: Packet*)
  extends Packet {
  
  // ---- Packet implementation ----
  def name: String = "#bundle" // Bundle.TAG

  @throws(classOf[Exception])
  def encode(c: PacketCodec, b: ByteBuffer): Unit = c.encodeBundle(this, b)

  def encodedSize(c: PacketCodec): Int = c.encodedBundleSize(this)

  private[osc] def printTextOn(c: PacketCodec, stream: PrintStream, nestCount: Int): Unit = {
    stream.print("  " * nestCount)
    stream.print(s"[ #bundle, $timeTag")
    val ncInc = nestCount + 1
    packets.foreach { v =>
      stream.println(',')
      v.printTextOn(c, stream, ncInc)
    }
    if (nestCount == 0) stream.println(" ]") else stream.print(" ]")
  }

  override def toString: String = {
    val tt = if (timeTag.raw == 1) "now"
    else {
      val secsSince1900 = (timeTag.raw >> 32) & 0xFFFFFFFFL
      if (secsSince1900 > TimeTag.SECONDS_FROM_1900_TO_1970) {
        s"millis(${timeTag.toMillis}L)"
      } else {
        s"secs(${timeTag.toSecs}"
      }
    }
    packets.mkString(s"Bundle.$tt(", ", ", ")")
  }
}

// ------------------------------

object Message {
  def apply(name: String, args: Any*): Message = new Message(name, args: _*)

  def unapplySeq(m: Message): Option[(String, Seq[Any])] = Some(m.name -> m.args)
}

class Message(val name: String, val args: Any*)
  extends Packet {

  import Packet._
   
  def encode(c: PacketCodec, b: ByteBuffer): Unit = c.encodeMessage(this, b)

  def encodedSize(c: PacketCodec): Int = c.encodedMessageSize(this)

  // recreate stuff we lost when removing case modifier
  override def toString: String =
    args.mkString(s"Message($name, ", ", ", ")")

  override def hashCode(): Int = name.hashCode * 41 + args.hashCode

  override def equals(other: Any): Boolean = other match {
    case that: Message  => (that isComparable this) && this.name == that.name && this.args == that.args
    case _              => false
  }

  protected def isComparable(other: Any): Boolean = other.isInstanceOf[Message]

	// ---- Packet implementation ----

  private[osc] def printTextOn(c: PacketCodec, stream: PrintStream, nestCount: Int): Unit = {
    stream.print("  " * nestCount)
    stream.print("[ ")
    printEscapedStringOn(stream, name)
    args.foreach { v =>
      stream.print(", ")
      // XXX eventually encoder and decoder should be strictly separated,
      // and hence we would integrate the printing of the incoming messages
      // directly into the decoder!
      //			c.atomEncoders( v.asInstanceOf[ AnyRef ].getClass ).printTextOn( c, stream, nestCount, v )

      //			c.atomEncoders( v ).printTextOn( c, stream, nestCount, v )
      c.printAtom(v, stream, nestCount)
    }
    if (nestCount == 0) stream.println(" ]") else stream.print(" ]")
  }
}