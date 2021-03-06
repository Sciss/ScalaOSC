/*
 * ClientImpl.scala
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

private[osc] trait ClientImpl[Address]
  extends Channel.Directed.Input with Channel.Directed.Output with DirectedImpl[Address] with BidiImpl {

  override protected def input : Channel.Directed.Input
  override protected def output: Channel.Directed.Output

  override def toString: String = s"${transport.name}.Client($target)@${hashCode().toHexString}"

  final def action      : Channel.Directed.Input.Action         = input.action
  final def action_=(fun: Channel.Directed.Input.Action): Unit  = input.action = fun

  final def ! (p: Packet): Unit = output ! p
}