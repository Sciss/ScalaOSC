package de.sciss.osc

import org.scalatest.{FeatureSpec, GivenWhenThen}
import java.nio.ByteBuffer

/**
* To run this test copy + paste the following into sbt:
* {{
* test-only de.sciss.osc.ScalaOSCSuite
* }}
*/
class ScalaOSCSuite extends FeatureSpec with GivenWhenThen {
   val NUM_MESSAGES  = 5000
   val MAX_ARGS      = 100
   val MAX_NAME      = 40
   val MAX_BLOB      = 50
   val RAND_SEED     = 0L
   val DUMP_IN       = false

   feature( "Conforming messages can be en- and decodec" ) {
      info( "Several random messages are generated" )
      info( "with args conforming to different codec specs" )

      val rnd = new util.Random( RAND_SEED )

      def scenarioWithTime( name: String, descr: String )( body: => Unit ) {
         scenario( descr ) {
            val t1 = System.currentTimeMillis()
            body
            val t2 = System.currentTimeMillis()
            println( "For " + name + " the tests took " + formatSeconds( (t2 - t1) * 0.001 ))
         }
      }

      def randName() : String = {
         val sz = rnd.nextInt( MAX_NAME - 1 ) + 1
         "/" + Seq.fill( sz )( (rnd.nextInt( 0x7F ) + 1).toChar ).mkString
      }

      def randInt() : Int     = rnd.nextInt()
      def randFloat() : Float = {
         if( rnd.nextDouble() > 0.02 ) rnd.nextFloat() * rnd.nextInt() else rnd.nextInt( 3 ) match {
            case 0 => Float.NegativeInfinity
            case 1 => Float.PositiveInfinity
            case 2 => Float.NaN
         }
      }
      def randString() : String = {
         Seq.fill[ Char ]( rnd.nextInt( MAX_NAME ))( if( rnd.nextDouble() > 0.1 ) (rnd.nextInt( 0x7F ) + 1).toChar else {
            "äöüÄÖÜß\u02D0\u0600\u0B90\u1830\u2121\u25A3€{}".charAt( rnd.nextInt( 16 ))
         }).mkString
      }
      def randBlob() : ByteBuffer = {
         val arr = new Array[ Byte ]( rnd.nextInt( MAX_BLOB ))
         rnd.nextBytes( arr )
         ByteBuffer.wrap( arr )
      }
      def wchoose[ T ]( seq: Traversable[ T ])( fun: T => Double ) : T = {
         val i    = rnd.nextDouble()
         var sum  = 0.0
         seq find { e => sum += fun( e ); sum >= i } getOrElse seq.last
      }
      def normalize[ A ]( seq: Seq[ (Int, A) ]) : Seq[ (Double, A) ] = {
         val sum  = seq.map( _._1 ).sum
         val f    = 1.0 / sum
         seq.map { case (a, b) => (a * f, b) }
      }
      val strictArgs = normalize(
         Seq[ (Int, () => Any) ]( 10 -> (randInt _), 10 -> (randFloat _), 10 -> (randString _), 1 -> (randBlob _) )
      )

      def randStrictArg() : Any = wchoose( strictArgs )( _._1 )._2()

      scenarioWithTime( "strict", "Strict 1.0 codec is verified" ) {
         val c    = PacketCodec().build
         val bb   = ByteBuffer.allocate( 8192 )

         info( "When several random messages are en- and decoded:" )
         info( "- the codec should be able to report the correct byte size prior encoding" )
         info( "- the decoded messages should be equal to their input" )
         info( "- it should be possible to re-encode and re-decode the decoded messages" )
         for( i <- 0 until NUM_MESSAGES ) {
            val numArgs = rnd.nextInt( MAX_ARGS )
            val in  = Message( randName(), Seq.fill( numArgs )( randStrictArg() ): _* )

//            when( "the codec is asked for the encoded message size" )
            val sz1 = c.encodedMessageSize( in )
//            then( "the result should be equal to the size of the actually encoded message" )
            bb.clear()
            in.encode( c, bb )
            bb.flip()
            val sz2 = bb.limit()

            if( DUMP_IN ) {
               in.printTextOn( c, Console.out, 0 )
               Packet.printHexOn( bb, Console.out )
            }

            assert( sz1 == sz2, "Reported message size " + sz1 + " != actual " + sz2 )

            val out = c.decode( bb ).asInstanceOf[ Message ]
//            when( "an original message is compared to the one that went through the codec" )
//            then( "they should be equal" )
            checkMessageEquality( in, out )

//            when( "a decoded message is received" )
//            then( "it should be possible to re-encode and then re-decode it" )
            bb.clear()
            out.encode( c, bb )
            bb.flip()
            val out2 = c.decode( bb ).asInstanceOf[ Message ]
//            then( "it should be equal to the input as well" )
            checkMessageEquality( in, out2 )
         }

         info( "When several unsupported types are put in a message:" )
         info( "- the encoder should fail to operate" )

         def throwAny( v: Any ) {
            bb.clear()
            c.encodeMessage( Message( "/test", v ), bb )
            fail( "Codec should not support argument '" + v + "'" )
         }

         try {
            throwAny( 12.34 )          // doubles not supported
            throwAny( 56 :: Nil )      // arrays not supported
            throwAny( false )          // booleans not supported
            throwAny( Message( "/m" )) // packets not supported
         } catch {
            case pce: PacketCodec.Exception =>  // expected
         }
      }

     scenario("Nested bundle coding") {
       val m1   = Message("/on")
       val m2   = Message("/tw")
       val m3   = Message("/th", Bundle.now(m1, m2))
       val b    = Bundle.now(m3)
       val bb   = ByteBuffer.allocate(8192)
       val c    = PacketCodec().packetsAsBlobs().build
       b.encode(c, bb)
       bb.flip()
       val res  = c.decode(bb)
       res match {
         case Bundle(Timetag.now, Message("/three", 5, 6, m1b: ByteBuffer)) =>
           c.decode(m1b) match {
             case Bundle(Timetag.now, `m1`, `m2`) =>
           }
       }
     }
   }

   def checkMessageEquality( a: Message, b: Message ) {
      assert( a.name == b.name, "Message names divert: '" + a.name + "' != '" + b.name + "'" )
      assert( a.size == b.size, "Message arg counts divert: " + a.size + " != " + b.size )
      a.args.zip( b.args ).foreach { case (aa, ba) =>
         val aaf = fixArgEquality( aa )
         val baf = fixArgEquality( ba )
         assert( aaf == baf, "Message arg diverts: '" + aaf + "' != '" + baf + "'" )
      }
   }

   def fixArgEquality( a: Any ) : Any = a match {
      case b: ByteBuffer => {
         val arr = new Array[ Byte ]( b.remaining() )
         val p   = b.position()
         b.get( arr )
         b.position( p )
         arr.toIndexedSeq
      }

      case f: Float if( f.isNaN ) => 0f   // two NaNs do not report to be equal!

      case _ => a
   }

   def formatSeconds( seconds: Double ) : String = {
      val millisR    = (seconds * 1000).toInt
      val sb         = new StringBuilder( 10 )
      val secsR      = millisR / 1000
      val millis     = millisR % 1000
      val mins       = secsR / 60
      val secs       = secsR % 60
      if( mins > 0 ) {
         sb.append( mins )
         sb.append( ':' )
         if( secs < 10 ) {
            sb.append( '0' )
         }
      }
      sb.append( secs )
      sb.append( '.' )
      if( millis < 10 ) {
         sb.append( '0' )
      }
      if( millis < 100 ) {
         sb.append( '0' )
      }
      sb.append( millis )
      sb.append( 's' )
      sb.toString()
   }
}