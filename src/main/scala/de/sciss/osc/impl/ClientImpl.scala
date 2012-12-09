package de.sciss.osc
package impl

private[osc] trait ClientImpl extends Client with DirectedImpl with BidiImpl {
   override protected def input: Channel.DirectedInput
   override protected def output: Channel.DirectedOutput

   override def toString = transport.name + ".Client(" + target + ")"

   final def action = input.action
   final def action_=( fun: Channel.DirectedInput.Action ) { input.action = fun }

   final def !( p: Packet ) { output ! p }
}