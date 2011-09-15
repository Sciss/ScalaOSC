package de.sciss.osc

object Dump {
   def apply( id: Int ) : Dump = id match {
      case Off.id    => Off
      case Text.id   => Text
      case Hex.id    => Hex
      case Both.id   => Both
      case _         => throw new IllegalArgumentException( id.toString )
   }

   /**
    *	Dump mode: do not dump messages
    */
   case object Off extends Dump  { val id = 0 }
   /**
    *	Dump mode: dump messages in text formatting
    */
   case object Text extends Dump { val id = 1 }
   /**
    *	Dump mode: dump messages in hex (binary) view
    */
   case object Hex extends Dump  { val id = 2 }
   /**
    *	Dump mode: dump messages both in text and hex view
    */
   case object Both extends Dump { val id = 3 }

   type Filter = Packet => Boolean
   val AllPackets : Filter = _ => true
}
sealed trait Dump { val id: Int }