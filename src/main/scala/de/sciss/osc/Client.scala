package de.sciss.osc

import java.net.SocketAddress

object Client {
   type Net = Client with Channel.Net.ConfigLike

   // convenient redirections

//   def apply( target: SocketAddress, config: UDP.Config ) : UDP.Client = UDP.Client( target, config )
//   def apply( target: SocketAddress, config: TCP.Config ) : TCP.Client = TCP.Client( target, config )

   def apply( target: SocketAddress, config: Channel.Net.Config ) : Client.Net = config match {
      case udp: UDP.Config => UDP.Client( target, udp )
      case tcp: TCP.Config => TCP.Client( target, tcp )
   }
}