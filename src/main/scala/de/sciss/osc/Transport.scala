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
import de.sciss.osc.{Receiver => OSCReceiver, Transmitter => OSCTransmitter}

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

   sealed trait Config extends Channel.NetConfig {
      override final def toString = name + ".Config"
      def openChannel() : DatagramChannel
   }
   sealed trait ConfigBuilder extends Channel.NetConfigBuilder {
      override final def toString = name + ".ConfigBuilder"
      override def build : Config
   }

   private final class ConfigBuilderImpl extends Channel.NetConfigBuilderImpl with ConfigBuilder {
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

      def apply() : Undirected = new UndirectedImpl( Config.default )
      def apply( config: Config ) : Undirected = new UndirectedImpl( config )
      def apply( target: SocketAddress ) : Directed = new DirectedImpl( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Directed = new DirectedImpl( target, config )

      private final class UndirectedImpl( protected val config: Config )
      extends Transmitter with OSCTransmitter.UndirectedNet {
         override def toString = name + ".Transmitter()"

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

      private final class DirectedImpl( target: SocketAddress, protected val config: Config )
      extends Transmitter with Channel.DirectedOutput with Channel.DirectedNet {
         override def toString = name + ".Transmitter()"

         @throws( classOf[ IOException ])
         def connect { channel.connect( target )}
         def isConnected = channel.isConnected
         def remoteSocketAddress = {
            val so = channel.socket()
            new InetSocketAddress( so.getInetAddress, so.getPort )
         }

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

   sealed trait Transmitter extends OSCTransmitter with Channel.NetConfigLike { // Channel.Net
      final override protected val channel: DatagramChannel = config.openChannel()
      override protected def config: Config
      final def transport = config.transport

      final def localSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
      }
   }

   private trait ChannelImpl extends OSCReceiver.Net {
      protected def config: Config
      final def transport = config.transport
      final protected val channel: DatagramChannel = config.openChannel()
      final def localSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
      }
      final def isConnected = channel.isConnected
      final def remoteSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getInetAddress, so.getPort )
      }
   }

   object Receiver {
      type Directed     = Receiver with OSCReceiver.Directed with OSCReceiver.Net
      type Undirected   = Receiver with OSCReceiver.UndirectedNet

      def apply() : Undirected = new UndirectedImpl( Config.default )
      def apply( config: Config ) : Undirected = new UndirectedImpl( config )
      def apply( target: SocketAddress ) : Directed = new DirectedImpl( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Directed = new DirectedImpl( target, config )

      private final class UndirectedImpl( protected val config: Config )
      extends Receiver with OSCReceiver.UndirectedNet with ChannelImpl {
         override def toString = name + ".Receiver()"

         protected def receive() {
            buf.clear()
            val sender = channel.receive( buf )
            flipDecodeDispatch( sender )
         }
      }

      private final class DirectedImpl( target: SocketAddress, protected val config: Config )
      extends Receiver with OSCReceiver.Directed with ChannelImpl {
         override def toString = name + ".Receiver(" + target + ")"

         protected def connectChannel() { channel.connect( target )}

         protected def receive() {
            buf.clear()
            if( channel.receive( buf ) != null ) flipDecodeDispatch()
         }
      }
   }

   sealed trait Receiver extends OSCReceiver with Channel.NetConfigLike {
//      protected def channel: DatagramChannel
      override protected def config: Config
   }
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
      def transport = UDP
      def build: Config = ConfigImpl( bufferSize, codec, localSocketAddress )
   }

   private final case class ConfigImpl( bufferSize: Int, codec: PacketCodec,
                                        localSocketAddress: InetSocketAddress )
   extends Config {
      def transport = UDP
      def openChannel() = {
         val ch = SocketChannel.open()
         ch.socket().bind( localSocketAddress )
         ch
      }
   }

   object Transmitter {
      def apply( target: SocketAddress ) : Transmitter = new Impl( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Transmitter = new Impl( target, config )

      private final class Impl( target: SocketAddress, protected val config: Config )
      extends Transmitter with ChannelImpl {
         override def toString = name + ".Transmitter(" + target + ")"

//         protected def config = cfg

         @throws( classOf[ IOException ])
         def connect { channel.connect( target )}

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

   private trait ChannelImpl {
      protected def config: Config
      final def transport = config.transport
      final protected val channel: SocketChannel = config.openChannel()
      final def localSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
      }
      def isConnected = channel.isConnected
      def remoteSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getInetAddress, so.getPort )
      }
   }

   sealed trait Transmitter extends OSCTransmitter with Channel.DirectedOutput with Channel.DirectedNet {
//      protected def channel: SocketChannel
      override protected def config: Config
   }

   object Receiver {
      def apply( target: SocketAddress ) : Receiver = new Impl( target, Config.default )
      def apply( target: SocketAddress, config: Config ) : Receiver = new Impl( target, config )

      private final class Impl( target: SocketAddress, protected val config: Config )
      extends Receiver with ChannelImpl {
         override def toString = name + ".Receiver(" + target + ")"

         @throws( classOf[ IOException ])
         protected def connectChannel { channel.connect( target )}

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

   sealed trait Receiver extends OSCReceiver.Directed with Channel.DirectedNet {
//      protected def channel: SocketChannel
      override protected def config: Config
   }
}

case object File extends Transport {
   val name = "File"
}