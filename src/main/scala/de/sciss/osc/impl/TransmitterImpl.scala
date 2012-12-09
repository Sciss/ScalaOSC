package de.sciss.osc
package impl

import java.io.IOException

private[osc] trait TransmitterImpl extends SingleOutputChannelImpl /* .Output */ {
   @throws( classOf[ IOException ])
   final def close() {
      channel.close()
   }

   @throws( classOf[ IOException ])
   final def connect() {
      connectChannel()
   }
}