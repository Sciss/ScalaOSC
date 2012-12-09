package de.sciss

package object osc {
   type Client = Channel.Bidi with Channel.Directed.Input with Channel.Directed.Output
}
