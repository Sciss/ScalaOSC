package de.sciss.osc
package impl

private[osc] trait DirectedInputImpl extends DirectedImpl with Channel.Directed.Input {
   final var action: Channel.Directed.Input.Action = Channel.Directed.Input.NoAction
}