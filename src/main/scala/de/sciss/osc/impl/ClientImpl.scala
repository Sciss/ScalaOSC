package de.sciss.osc
package impl

private[osc] trait ClientImpl extends Channel.Directed.Input with Channel.Directed.Output with DirectedImpl with BidiImpl {
   override protected def input:  Channel.Directed.Input
   override protected def output: Channel.Directed.Output

   override def toString = transport.name + ".Client(" + target + ")@" + hashCode().toHexString

   final def action = input.action
   final def action_=( fun: Channel.Directed.Input.Action ) { input.action = fun }

   final def !( p: Packet ) { output ! p }
}