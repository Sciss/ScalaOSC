/*
 * DirectedInputImpl.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2021 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc
package impl

private[osc] trait DirectedInputImpl[Address] extends DirectedImpl[Address] with Channel.Directed.Input {
  final var action: Channel.Directed.Input.Action = Channel.Directed.Input.NoAction
}