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
                     buf.clear()
                     p.encode( codec, buf )
                     buf.flip()
                     dumpPacket( p )
                     channel.send( buf, target )
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
                     buf.clear()
                     p.encode( codec, buf )
                     buf.flip()
                     dumpPacket( p )
                     channel.write( buf )
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
         new Transmitter with ChannelImpl {
            override def toString = name + ".Transmitter(" + target + ")"

            protected def config = cfg

            @throws( classOf[ IOException ])
            def connect { channel.connect( target )}

            @throws( classOf[ IOException ])
            def !( p: OSCPacket ) {
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
                      case m: OSCMessage => m.name
                      case _ => p.getClass.getName
                   })
               }
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

   sealed trait Transmitter extends OSCTransmitter.Directed with OSCChannel.DirectedNet {
//      protected def channel: SocketChannel
      override protected def config: Config
   }

   object Receiver {
      def apply( target: SocketAddress )( implicit config: Config ) : Receiver = {
         val cfg = config
         new Receiver with ChannelImpl {
            override def toString = name + ".Receiver(" + target + ")"

            def config = cfg

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

               flipDecodeDispatch( target )
            }
         }
      }
   }

   sealed trait Receiver extends OSCReceiver with OSCChannel.DirectedNet {
//      protected def channel: SocketChannel
      override protected def config: Config
   }
}

case object OSCFileTransport extends OSCTransport {
   val name = "File"
}