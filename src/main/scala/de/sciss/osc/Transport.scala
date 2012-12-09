/*
 * Transport.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2012 Hanns Holger Rutz. All rights reserved.
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

import java.io.IOException
import java.nio.channels.{SocketChannel, DatagramChannel}
import de.sciss.osc.{Channel => OSCChannel, Client => OSCClient,
   Receiver => OSCReceiver, Transmitter => OSCTransmitter}
import java.net.{InetAddress, SocketAddress, InetSocketAddress}

sealed trait Transport { def name: String }

object Transport {
   sealed trait Net extends Transport

   def apply( name: String ) : Transport = name.toUpperCase match {
      case UDP.name  => UDP
      case TCP.name  => TCP
      case File.name => File
      case _         => throw new IllegalArgumentException( name )
   }
}

case object UDP extends Transport.Net {
   final val name = "UDP"

   object Config {
      def default : Config = apply().build
      implicit def build( b: ConfigBuilder ) : Config = b.build
      def apply() : ConfigBuilder = new impl.UDPConfigBuilderImpl
   }

   trait Config extends OSCChannel.Net.Config {
      override final def toString = name + ".Config"
      def openChannel( discardWildcard: Boolean = false ) : DatagramChannel
   }
   trait ConfigBuilder extends OSCChannel.Net.ConfigBuilder {
      override final def toString = name + ".ConfigBuilder"
      override def build : Config
   }

   object Transmitter {
      type Directed     = OSCTransmitter.DirectedNet with Channel
      type Undirected   = OSCTransmitter.UndirectedNet with Channel

      def apply() : Undirected = apply( Config.default )
      def apply( config: Config ) : Undirected = apply( config.openChannel( discardWildcard = true ), config )
//      def apply( channel: DatagramChannel ) : Undirected = apply( channel, Config.default )
//      def apply( channel: Channel ) : Undirected = apply( channel.channel, channel.config )
      private[osc] def apply( channel: DatagramChannel, config: Config ) : Undirected =
         new impl.UndirectedUDPTransmitterImpl( channel, config )

      def apply( target: SocketAddress ) : Directed = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Directed = apply( config.openChannel(), target, config )
//      def apply( channel: DatagramChannel, target: SocketAddress ) : Directed = apply( channel, target, Config.default )
//      def apply( rcv: Receiver.Directed ) : Directed = apply( rcv.channel, rcv.target, rcv.config )
      private[osc] def apply( channel: DatagramChannel, target: SocketAddress, config: Config ) : Directed =
         new impl.DirectedUDPTransmitterImpl( channel, target, config )
   }

//   sealed trait Transmitter extends OSCTransmitter with Channel
////   {
////      override def channel: DatagramChannel
////   }

   trait Channel extends OSCChannel with OSCChannel.Net.ConfigLike {
      override def channel: DatagramChannel
   }

   object Receiver {
      type Directed     = OSCReceiver.Directed with OSCReceiver.Net
      type Undirected   = OSCReceiver.Undirected.Net

      def apply() : Undirected = apply( Config.default )
      def apply( config: Config ) : Undirected = apply( config.openChannel(), config )
      def apply( channel: DatagramChannel ) : Undirected = apply( channel, Config.default )
      def apply( channel: DatagramChannel, config: Config ) : Undirected =
         new impl.UndirectedUDPReceiverImpl( channel, config )

      def apply( target: SocketAddress ) : Directed = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Directed = apply( config.openChannel(), target, config )
      def apply( channel: DatagramChannel, target: SocketAddress ) : Directed = apply( channel, target, Config.default )
      def apply( channel: DatagramChannel, target: SocketAddress, config: Config ) : Directed =
         new impl.DirectedUDPReceiverImpl( channel, target, config )
   }

   object Client {
      def apply( target: SocketAddress ) : Client = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Client =
         new impl.UDPClientImpl( config.openChannel( discardWildcard = true ), target, config )
   }

   type Client = OSCClient with Channel

//   sealed trait Client extends OSCClient with Channel
////   {
////      override protected def config: Config
////   }
}

/**
 * `TCP` as a transport for OSC. At the moment, packets
 * are encoded in the OSC 1.0 format, regardless of
 * of the configuration's packet codec. That means
 * the 32-bit Int size followed by the actual plain packet is
 * encoded. The OSC 1.1 draft suggests SLIP
 * (cf. http://www.faqs.org/rfcs/rfc1055.html).
 * This may be configurable in the future.
 */
case object TCP extends Transport.Net {
   final val name = "TCP"

   object Config {
      def default : Config = apply().build
      implicit def build( b: ConfigBuilder ) : Config = b.build
      def apply() : ConfigBuilder = new impl.TCPConfigBuilderImpl
   }

   trait Config extends Channel.Net.Config {
      override final def toString = name + ".Config"
      def openChannel( discardWildcard: Boolean = true ) : SocketChannel
   }
   trait ConfigBuilder extends Channel.Net.ConfigBuilder {
      override final def toString = name + ".ConfigBuilder"
      override def build : Config
   }

   object Transmitter {
      def apply( target: SocketAddress ) : Transmitter = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Transmitter =
         apply( config.openChannel( discardWildcard = true ), target, config )

      private[osc] def apply( channel: SocketChannel, target: SocketAddress, config: Config ) : Transmitter =
         new impl.TCPTransmitterImpl( channel, target, config )
   }

   trait Channel extends OSCChannel.DirectedNet {
      override def channel: SocketChannel
   }

   type Transmitter = OSCChannel.DirectedOutput

//   sealed trait Transmitter
//   extends OSCTransmitter with OSCChannel.DirectedOutput with Channel
////   {
//////      protected def channel: SocketChannel
////      override protected def config: Config
////   }

   object Receiver {
      def apply( target: SocketAddress ) : Receiver = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Receiver =
         apply( config.openChannel( discardWildcard = true ), target, config )

      def apply( channel: SocketChannel, target: SocketAddress ) : Receiver = apply( channel, target, Config.default )
      def apply( channel: SocketChannel, target: SocketAddress, config: Config ) : Receiver =
         new impl.TCPReceiverImpl( channel, target, config )
   }

//   sealed trait Receiver extends OSCReceiver.DirectedImpl with Channel
   type Receiver = OSCReceiver.Directed

   object Client {
      def apply( target: SocketAddress ) : Client = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Client =
         new impl.TCPClientImpl( config.openChannel( discardWildcard = true ), target, config )
   }

   type Client = OSCClient with Channel
}

/**
 * XXX TODO -- this transport has not yet been implemented.
 */
case object File extends Transport {
   val name = "File"
}