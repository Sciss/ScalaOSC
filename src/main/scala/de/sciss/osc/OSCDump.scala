package de.sciss.osc

object OSCDump {
   def apply( id: Int ) : OSCDump = id match {
      case Off.id    => Off
      case Text.id   => Text
      case Hex.id    => Hex
      case Both.id   => Both
      case _         => throw new IllegalArgumentException( id.toString )
   }

   /**
    *	Dump mode: do not dump messages
    */
   case object Off extends OSCDump  { val id = 0 }
   /**
    *	Dump mode: dump messages in text formatting
    */
   case object Text extends OSCDump { val id = 1 }
   /**
    *	Dump mode: dump messages in hex (binary) view
    */
   case object Hex extends OSCDump  { val id = 2 }
   /**
    *	Dump mode: dump messages both in text and hex view
    */
   case object Both extends OSCDump { val id = 3 }
}
sealed trait OSCDump { val id: Int }