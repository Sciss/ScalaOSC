package de.sciss.osc
package impl

private[osc] trait DirectedInputImpl extends DirectedImpl with Channel.DirectedInput {
   final var action: Channel.DirectedInput.Action = Channel.DirectedInput.NoAction
}