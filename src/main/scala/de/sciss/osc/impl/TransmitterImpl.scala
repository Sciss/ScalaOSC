/*
 * TransmitterImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2015 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc
package impl

import java.io.IOException

private[osc] trait TransmitterImpl extends SingleOutputChannelImpl {
  @throws(classOf[IOException])
  final def close(): Unit = channel.close()

  @throws(classOf[IOException])
  final def connect(): Unit = connectChannel()
}