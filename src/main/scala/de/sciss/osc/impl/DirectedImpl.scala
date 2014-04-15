/*
 * DirectedImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2014 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc
package impl

import java.net.SocketAddress

private[osc] trait DirectedImpl extends ChannelImpl {
  protected def target: SocketAddress
}