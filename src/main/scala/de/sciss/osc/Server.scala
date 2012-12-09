package de.sciss.osc

object Server {
   type Net = Server with Channel.Net.ConfigLike

   def apply( config: TCP.Config ) : Server.Net = TCP.Server( config )
}
trait Server extends Channel.Bidi {
   type Connection <: Channel.Directed.Output

   type Action = (Packet, Connection) => Unit

   def action: Action
   def action_=( value: Action ) : Unit

//   @throws( classOf[ IOException ])
//   def send( p: Packet, target: Connection ) : Unit

//   def config: TCP.Config
}