package de.sciss.osc

import java.net.SocketAddress

object Client {
   // convenient redirections

   def apply( target: SocketAddress, config: UDP.Config ) : UDP.Client = UDP.Client( target, config )
   def apply( target: SocketAddress, config: TCP.Config ) : TCP.Client = TCP.Client( target, config )
}