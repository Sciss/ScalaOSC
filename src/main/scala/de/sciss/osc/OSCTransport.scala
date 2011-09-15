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

sealed trait OSCTransport { def name: String }

object OSCTransport {
   sealed trait Net extends OSCTransport

   def apply( name: String ) : OSCTransport = name.toUpperCase match {
      case UDP.name              => UDP
      case TCP.name              => TCP
      case OSCFileTransport.name => OSCFileTransport
      case _                     => throw new IllegalArgumentException( name )
   }
}

case object UDP extends OSCTransport.Net {
   val name = "UDP"

   object Config {
      implicit def default = apply().build
      def apply() : ConfigBuilder = new ConfigBuilderImpl
   }

   sealed trait Config extends OSCChannel.NetConfig {
      def openChannel() : DatagramChannel
   }
   sealed trait ConfigBuilder extends OSCChannel.NetConfigBuilder {
      override def build : Config
   }

   private final class ConfigBuilderImpl extends OSCChannel.NetConfigBuilderImpl with ConfigBuilder {
      def transport = UDP
      def build: Config = ConfigImpl( bufferSize, codec, localSocketAddress )
   }

   private final case class ConfigImpl( bufferSize: Int, codec: OSCPacketCodec,
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

      def apply( implicit config: Config ) : Undirected = {
         val cfg = config
         new Transmitter with OSCTransmitter.UndirectedNet {
            override def toString = name + ".Transmitter()"
            protected def config = cfg

            @throws( classOf[ IOException ])
            def send( p: OSCPacket, target: SocketAddress ) {
               try {
                  bufSync.synchronized {
                     byteBuf.clear()
                     p.encode( codec, byteBuf )
                     byteBuf.flip()
                     dumpPacket( p )
                     channel.send( byteBuf, target )
                  }
               }
               catch { case e: BufferOverflowException =>
                   throw new OSCException( OSCException.BUFFER, p match {
                      case m: OSCMessage => m.name
                      case _ => p.getClass.getName
                   })
               }
            }
         }
      }

      def apply( target: SocketAddress )( implicit config: Config ) : Directed = {
         val cfg = config
         new Transmitter with OSCTransmitter.Directed with OSCChannel.DirectedNet {
            override def toString = name + ".Transmitter()"
            protected def config = cfg

            @throws( classOf[ IOException ])
            def connect { channel.connect( target )}
            def isConnected = channel.isConnected
            def remoteSocketAddress = {
               val so = channel.socket()
               new InetSocketAddress( so.getInetAddress, so.getPort )
            }

            @throws( classOf[ IOException ])
            def !( p: OSCPacket ) {
               try {
                  bufSync.synchronized {
                     byteBuf.clear()
                     p.encode( codec, byteBuf )
                     byteBuf.flip()
                     dumpPacket( p )
                     channel.write( byteBuf )
                  }
               }
               catch { case e: BufferOverflowException =>
                   throw new OSCException( OSCException.BUFFER, p match {
                      case m: OSCMessage => m.name
                      case _ => p.getClass.getName
                   })
               }
            }
         }
      }
   }

   sealed trait Transmitter extends OSCTransmitter with OSCChannel.NetConfigLike { // OSCChannel.Net
      final override protected val channel: DatagramChannel = config.openChannel()
      override protected def config: Config
      final def transport = config.transport

      final def localSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
      }
   }
}

case object TCP extends OSCTransport.Net {
   val name = "TCP"

   object Config {
      implicit def default = apply().build
      def apply() : ConfigBuilder = new ConfigBuilderImpl
   }

   sealed trait Config extends OSCChannel.NetConfig {
      def openChannel() : SocketChannel
   }
   sealed trait ConfigBuilder extends OSCChannel.NetConfigBuilder {
      override def build : Config
   }

   private final class ConfigBuilderImpl extends OSCChannel.NetConfigBuilderImpl with ConfigBuilder {
      def transport = UDP
      def build: Config = ConfigImpl( bufferSize, codec, localSocketAddress )
   }

   private final case class ConfigImpl( bufferSize: Int, codec: OSCPacketCodec,
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
      def apply( target: SocketAddress )( implicit config: Config ) : Transmitter = {
         val cfg = config
         new Transmitter {
            override def toString = name + ".Transmitter(" + target + ")"

            protected def config = cfg

            @throws( classOf[ IOException ])
            def connect { channel.connect( target )}
            def isConnected = channel.isConnected
            def remoteSocketAddress = {
               val so = channel.socket()
               new InetSocketAddress( so.getInetAddress, so.getPort )
            }

            @throws( classOf[ IOException ])
            def !( p: OSCPacket ) {
               try {
                  bufSync.synchronized {
                     byteBuf.clear()
                     byteBuf.position( 4 )
                     p.encode( codec, byteBuf )
                     val len = byteBuf.position() - 4
                     byteBuf.flip()
                     byteBuf.putInt( 0, len )
                     dumpPacket( p )
                     channel.write( byteBuf )
                  }
               }
               catch { case e: BufferOverflowException =>
                   throw new OSCException( OSCException.BUFFER, p match {
                      case m: OSCMessage => m.name
                      case _ => p.getClass.getName
                   })
               }
            }
         }
      }
   }

   sealed trait Transmitter extends OSCTransmitter.Directed with OSCChannel.DirectedNet {
      final def transport = config.transport

      final def localSocketAddress = {
         val so = channel.socket()
         new InetSocketAddress( so.getLocalAddress, so.getLocalPort )
      }

      final override protected val channel: SocketChannel = config.openChannel()
      override protected def config: Config
   }

   object Receiver {
      def apply( target: SocketAddress )( implicit config: Config ) : Transmitter = {
         sys.error( "TODO" )
      }
   }

   sealed trait Receiver extends OSCReceiver with OSCChannel.DirectedNet {

   }
}

case object OSCFileTransport extends OSCTransport {
   val name = "File"
}