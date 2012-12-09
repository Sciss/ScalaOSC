package de.sciss.osc
package impl

import java.net.{InetAddress, InetSocketAddress}

private[osc] trait NetChannelConfigBuilderImpl extends ChannelConfigBuilderImpl with Channel.Net.ConfigBuilder {
   private var localSocket       = new InetSocketAddress( "0.0.0.0", 0 )
   final def localSocketAddress  = localSocket
   final def localSocketAddress_=( addr: InetSocketAddress ) { localSocket = addr }

   final def localPort_=( port: Int ) {
      localSocket = new InetSocketAddress( localSocket.getAddress, port )
   }

   final def localAddress_=( address: InetAddress ) {
      localSocket = new InetSocketAddress( address, localSocket.getPort )
   }

   final def localIsLoopback_=( loopback: Boolean ) {
      if( localSocket.getAddress.isLoopbackAddress != loopback ) {
         localAddress = InetAddress.getByName( if( loopback ) "127.0.0.1" else "0.0.0.0" )
      }
   }
}

