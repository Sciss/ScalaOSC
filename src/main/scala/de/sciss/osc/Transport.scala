/*
 * OSCTransport.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2011 Hanns Holger Rutz. All rights reserved.
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

import java.nio.BufferOverflowException
import java.net.{SocketAddress, InetSocketAddress}
import java.io.IOException
import java.nio.channels.{SocketChannel, DatagramChannel}
import de.sciss.osc.{Channel => OSCChannel, Client => OSCClient,
   Receiver => OSCReceiver, Transmitter => OSCTransmitter}

sealed trait Transport { def name: String }

object Transport {
   sealed trait Net extends Transport

   def apply( name: String ) : Transport = name.toUpperCase match {
      case UDP.name              => UDP
      case TCP.name              => TCP
      case File.name => File
      case _                     => throw new IllegalArgumentException( name )
   }
}

case object UDP extends Transport.Net {
   val name = "UDP"

   object Config {
      def default : Config = apply().build
      implicit def build( b: ConfigBuilder ) : Config = b.build
      def apply() : ConfigBuilder = new ConfigBuilderImpl
   }

   sealed trait Config extends OSCChannel.NetConfig {
      override final def toString = name + ".Config"
      def openChannel() : DatagramChannel
   }
   sealed trait ConfigBuilder extends OSCChannel.NetConfigBuilder {
      override final def toString = name + ".ConfigBuilder"
      override def build : Config
   }

   private final class ConfigBuilderImpl extends OSCChannel.NetConfigBuilderImpl with ConfigBuilder {
      def transport = UDP
      def build: Config = ConfigImpl( bufferSize, codec, localSocketAddress )
   }

   private final case class ConfigImpl( bufferSize: Int, codec: PacketCodec,
                                        localSocketAddress: InetSocketAddress )
   extends Config {
      def transport = UDP
      def openChannel() = {
         val ch = DatagramChannel.open()
         ch.socket().bind( localSocketAddress )
         ch
      }
   }

   object Transmitter {
      type Directed     = Transmitter with OSCTransmitter.DirectedNet
      type Undirected   = Transmitter with OSCTransmitter.UndirectedNet

      def apply() : Undirected = apply( Config.default )
      def apply( config: Config ) : Undirected = apply( config.openChannel(), config )
      def apply( channel: DatagramChannel ) : Undirected = apply( channel, Config.default )
      def apply( channel: DatagramChannel, config: Config ) : Undirected =
         new UndirectedImpl( channel, config )

      def apply( target: SocketAddress ) : Directed = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Directed = apply( config.openChannel(), target, config )
      def apply( channel: DatagramChannel, target: SocketAddress ) : Directed = apply( channel, target, Config.default )
      def apply( channel: DatagramChannel, target: SocketAddress, config: Config ) : Directed =
         new DirectedImpl( channel, target, config )

      private final class UndirectedImpl( val channel: DatagramChannel,
                                          protected val config: Config )
      extends Transmitter with OSCTransmitter.UndirectedNet {
         override def toString = name + ".Transmitter()"

//         def isConnected = channel.isOpen
//         protected def connectChannel() {}
//         def isConnected = isOpen

         @throws( classOf[ IOException ])
         def send( p: Packet, target: SocketAddress ) {
            try {
               bufSync.synchronized {
                  buf.clear()
                  p.encode( codec, buf )
                  buf.flip()
                  dumpPacket( p )
                  channel.send( buf, target )
               }
            }
            catch { case e: BufferOverflowException =>
                throw new OSCException( OSCException.BUFFER, p match {
                   case m: Message => m.name
                   case _ => p.getClass.getName
                })
            }
         }
      }

      private final class DirectedImpl( val channel: DatagramChannel,
                                        target: SocketAddress,
                                        protected val config: Config )
      extends Transmitter with OSCChannel.DirectedOutput with OSCChannel.DirectedNet {
         override def toString = name + ".Transmitter()"

         @throws( classOf[ IOException ])
         protected def connectChannel { if( !isConnected ) channel.connect( target )}
         def isConnected = channel.isConnected
//         def remoteSocketAddress = {
//            val so = channel.socket()
//            new InetSocketAddress( so.getInetAddress, so.getPort )
//         }

         @throws( classOf[ IOException ])
         def !( p: Packet ) {
            try {
               bufSync.synchronized {
                  buf.clear()
                  p.encode( codec, buf )
                  buf.flip()
                  dumpPacket( p )
                  channel.write( buf )
               }
            }
            catch { case e: BufferOverflowException =>
                throw new OSCException( OSCException.BUFFER, p match {
                   case m: Message => m.name
                   case _ => p.getClass.getName
                })
            }
         }
      }
   }

   sealed trait Transmitter extends OSCTransmitter with Channel
//   {
//      override def channel: DatagramChannel
//   }

   sealed trait Channel extends OSCChannel with OSCChannel.NetConfigLike {
      override protected def config: Config
      final def transport = config.transport
      override def channel: DatagramChannel
//      final protected val channel: DatagramChannel = config.openChannel()
      final def localSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
      }
//      final def isConnected = channel.isConnected
      final def remoteSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getInetAddress, so.getPort )
      }
   }

   object Receiver {
      type Directed     = Receiver with OSCReceiver.Directed with OSCReceiver.Net
      type Undirected   = Receiver with OSCReceiver.UndirectedNet

      def apply() : Undirected = apply( Config.default )
      def apply( config: Config ) : Undirected = apply( config.openChannel(), config )
      def apply( channel: DatagramChannel ) : Undirected = apply( channel, Config.default )
      def apply( channel: DatagramChannel, config: Config ) : Undirected =
         new UndirectedImpl( channel, config )

      def apply( target: SocketAddress ) : Directed = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Directed = apply( config.openChannel(), target, config )
      def apply( channel: DatagramChannel, target: SocketAddress ) : Directed = apply( channel, target, Config.default )
      def apply( channel: DatagramChannel, target: SocketAddress, config: Config ) : Directed =
         new DirectedImpl( channel, target, config )

      private final class UndirectedImpl( val channel: DatagramChannel,
                                          protected val config: Config )
      extends Receiver with OSCReceiver.UndirectedNet {
         override def toString = name + ".Receiver()"

         protected def receive() {
            buf.clear()
            val sender = channel.receive( buf )
            flipDecodeDispatch( sender )
         }
      }

      private final class DirectedImpl( val channel: DatagramChannel,
                                        target: SocketAddress,
                                        protected val config: Config )
      extends Receiver with OSCReceiver.DirectedImpl {
         override def toString = name + ".Receiver(" + target + ")"

         protected def connectChannel() { if( !isConnected ) channel.connect( target )}
         def isConnected = channel.isConnected

         protected def receive() {
            buf.clear()
            if( channel.receive( buf ) != null ) flipDecodeDispatch()
         }
      }
   }

   sealed trait Receiver extends OSCReceiver with Channel
//   {
//      override def channel: DatagramChannel
//   }

   object Client {
      def apply( target: SocketAddress ) : Client = apply( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Client =
         new Impl( config.openChannel(), target, config )

      private final class Impl( val channel: DatagramChannel,
                                target: SocketAddress,
                                protected val config: Config )
      extends Client with Channel {
         protected val input  = Receiver( channel, target, config )
         protected val output = Transmitter( channel, target, config )

         def !( p: Packet ) { output ! p }
      }
   }

   sealed trait Client extends OSCClient with Channel
//   {
//      override protected def config: Config
//   }
}

case object TCP extends Transport.Net {
   val name = "TCP"

   object Config {
      def default : Config = apply().build
      implicit def build( b: ConfigBuilder ) : Config = b.build
      def apply() : ConfigBuilder = new ConfigBuilderImpl
   }

   sealed trait Config extends Channel.NetConfig {
      override final def toString = name + ".Config"
      def openChannel() : SocketChannel
   }
   sealed trait ConfigBuilder extends Channel.NetConfigBuilder {
      override final def toString = name + ".ConfigBuilder"
      override def build : Config
   }

   private final class ConfigBuilderImpl extends Channel.NetConfigBuilderImpl with ConfigBuilder {
      def transport = TCP
      def build: Config = ConfigImpl( bufferSize, codec, localSocketAddress )
   }

   private final case class ConfigImpl( bufferSize: Int, codec: PacketCodec,
                                        localSocketAddress: InetSocketAddress )
   extends Config {
      def transport = TCP
      def openChannel() = {
         val ch = SocketChannel.open()
         ch.socket().bind( localSocketAddress )
         ch
      }
   }

   object Transmitter {
      def apply( target: SocketAddress ) : Transmitter = new Impl( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Transmitter = new Impl( target, config )

      private final class Impl( protected val target: SocketAddress, protected val config: Config )
      extends Transmitter {
         override def toString = name + ".Transmitter(" + target + ")"

//         protected def config = cfg

//         @throws( classOf[ IOException ])
//         protected def connectChannel { channel.connect( target )}

         @throws( classOf[ IOException ])
         def !( p: Packet ) {
            try {
               bufSync.synchronized {
                  buf.clear()
                  buf.position( 4 )
                  p.encode( codec, buf )
                  val len = buf.position() - 4
                  buf.flip()
                  buf.putInt( 0, len )
                  dumpPacket( p )
                  channel.write( buf )
               }
            }
            catch { case e: BufferOverflowException =>
                throw new OSCException( OSCException.BUFFER, p match {
                   case m: Message => m.name
                   case _ => p.getClass.getName
                })
            }
         }
      }
   }

   sealed trait Channel extends OSCChannel.DirectedNet {
      override protected def config: Config
      final def transport = config.transport
      final override val channel: SocketChannel = config.openChannel()
      final def localSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
      }
      protected def target: SocketAddress
      final protected def connectChannel() { if( !isConnected ) channel.connect( target )}
      final def isConnected = channel.isConnected
      def remoteSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getInetAddress, so.getPort )
      }
   }

   sealed trait Transmitter
   extends OSCTransmitter with OSCChannel.DirectedOutput with Channel
//   {
////      protected def channel: SocketChannel
//      override protected def config: Config
//   }

   object Receiver {
      def apply( target: SocketAddress ) : Receiver = new Impl( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Receiver = new Impl( target, config )

      private final class Impl( protected val target: SocketAddress, protected val config: Config )
      extends Receiver {
         override def toString = name + ".Receiver(" + target + ")"

//         @throws( classOf[ IOException ])
//         protected def connectChannel { channel.connect( target )}

         protected def receive() {
            buf.rewind().limit( 4 )	// in TCP mode, first four bytes are packet size in bytes
            do {
               val len = channel.read( buf )
               if( len == -1 ) return
            } while( buf.hasRemaining )

            buf.rewind()
            val packetSize = buf.getInt()
            buf.rewind().limit( packetSize )

            while( buf.hasRemaining ) {
               val len = channel.read( buf )
               if( len == -1 ) return
            }

            flipDecodeDispatch()
         }
      }
   }

   sealed trait Receiver extends OSCReceiver.DirectedImpl with Channel
//   {
////      protected def channel: SocketChannel
//      override protected def config: Config
//   }
}

/**
 * XXX TODO -- this transport has not yet been implemented.
 */
case object File extends Transport {
   val name = "File"
}