package de.sciss.osc
package impl

private[osc] trait ChannelConfigBuilderImpl extends Channel.ConfigBuilder {
   private var bufferSizeVar  = 8192
   final def bufferSize = bufferSizeVar
   final def bufferSize_=( size: Int ) {
      require( size >= 16, "Buffer size (" + size + ") must be >= 16" )
      bufferSizeVar = size
   }
   final var codec : PacketCodec = PacketCodec.default
}