package de.sciss

package object osc {
   type Client = Channel.Bidi with Channel.Directed.Input         with Channel.Directed.Output
//   type Server = Channel.Bidi with Channel.Undirected.Input.Net   with Channel.Directed.Output
}
