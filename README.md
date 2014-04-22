# ScalaOSC

## statement

ScalaOSC is an OpenSoundControl (OSC) library for the Scala programming language. It is (C)opyright 2008&ndash;2014 by Hanns Holger Rutz. All rights reserved. ScalaOSC is released under the [GNU Lesser General Public License](https://raw.github.com/Sciss/ScalaOSC/master/LICENSE) v2.1+ and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## requirements / installation

ScalaOSC currently builds against Scala 2.10 using sbt 0.13. It uses the I/O API from Java 1.6. Use the [NetUtil](http://www.sciss.de/netutil/) Java OSC library if you require compatibility with Java SE 1.4.

To link to ScalaOSC:

    libraryDependencies += "de.sciss" %% "scalaosc" % v

The current version `v` is `"1.1.3+"`

## overview

OpenSoundControl (OSC) is a protocol to exchange messages between systems, typically over a network using UDP or TCP, and typically to control sound or multimedia applications.

OSC can be used to control sound software &ndash; for example [SuperCollider Server](http://supercollider.sf.net/) was one of the first systems to use OSC &ndash;, but also to communicate with hardware controllers.

OSC is a generic protocol and not restricted to sound applications: For example, [SwingOSC](http://www.sciss.de/swingOSC/) uses OSC to provide network access to the Java Virtual Machine.

For more information, known implementations and the protocol standard, visit [opensoundcontrol.org](http://opensoundcontrol.org/).

## implementation

ScalaOSC currently provides single ended channels (`Transmitter` to send messages and `Receiver` to run a receiving loop), as well as single sockets in bidirectional mode (`Client` and `Server`). The supported transports are UDP and TCP (`Server` obviously requires TCP).

ScalaOSC comes with a codec conforming with the strict [OSC 1.0 specification](http://opensoundcontrol.org/spec-1_0). It can be configured to use the types `h` (64-bit integer), `d` (64-bit floating point), to encode OSC packets themselves as `b` blobs (as used by SuperCollider), to support the boolean tags `T` and `F` and many other optional types of OSC 1.1, including array wrapping with `[` and `]`. The API also allows to extend the codec with custom types.

## documentation and examples

An example of setting up a client that talks to the SuperCollider server running on UDP port 57110:

```scala
    
    import de.sciss.osc._
    import Implicits._      // simply socket address construction
    
    // create explicit config, because we want to customize it
    val cfg = UDP.Config()  
    // while SuperCollider uses only OSC 1.0 syntax, we want to
    // be able to use doubles and booleans, by making them fall
    // back to floats and 0/1 ints
    cfg.codec = PacketCodec().doublesAsFloats().booleansAsInts()
    
    // create a client talking to localhost port 57110. the client
    // picks a random free port for itself, unless you set it
    // explictly throught cfg.localPort = ...
    val c = UDP.Client( localhost -> 57110, cfg )
    
    // the following command actually establishes the connection
    c.connect()
    
    // now send out a few messages, step-by-step:
    c ! Message( "/s_new", "default", 1000 )
    c ! Message( "/n_set", 1000, "freq", 666.6 )
    c ! Message( "/n_run", 1000, false )
    c ! Message( "/n_run", 1000, true )
    c ! Message( "/n_free", 1000 )
    
    // finally shut down the client
    c.close()
    
````

Another very brief example, showing two UDP clients playing ping-pong:

```scala
    
    import de.sciss.osc
    // a sender, no target
    val pingT = osc.UDP.Transmitter()
    // a receiver, no target, but same channel as sender
    val pingR = osc.UDP.Receiver( pingT.channel )
    // a bidirectional client, targeted at ping's socket
    val pong = osc.UDP.Client( pingT.localSocketAddress )
    pingT.connect() // connect all channels
    pingR.connect()
    pong.connect()
    
    val t = new java.util.Timer()
    def delay( code: => Unit ) {
       t.schedule( new java.util.TimerTask {
          def run { code }}, 500 )
    }
    
    // unbound channels action takes packet and sender
    pingR.action = {
       // match against a particular message
       case (m @ osc.Message( "/ping", c: Int ), s) =>
          println( "Ping received " + m )
          delay {
             pingT.send( osc.Message( "/pong", c ), s )
         }
       case _ => // ignore any other message
    }
    
    // bound channels action takes just packet
    var cnt = 0
    pong.action = packet => {
       println( "Pong received " + packet )
       cnt += 1
       if( cnt <= 10 ) {
          // bound channels send via !(packet)
          delay { pong ! osc.Message( "/ping", cnt )}
       } else {
          pingR.close()
          pingT.close()
          pong.close()
          sys.exit( 0 )
       }
    }
    
    // unbound channels send via send(packet, addr)
    pingT.send( osc.Message( "/start" ),
       pong.localSocketAddress )
    
````

Another example, building a TCP echo server:

```scala

    import de.sciss.osc._

    val server = TCP.Server()
    server.action = {
      case (Message( name, args @ _* ), from) =>
        from ! Message( "/pong", args: _* )
    }
    server.connect()

    val client = TCP.Client( server.localSocketAddress )
    client.dump()
    client.connect()
    client ! Message( "/ping", 1, 2.3f )
````

Further examples can be found in the headers of the API docs, e.g. by looking up the documentation for `UDP.Client`.

ScalaOSC is used in the [ScalaCollider](http://www.sciss.de/scalaCollider/) project, so you may take a look at its usage there.

## todo

The TCP server uses one thread per connection right now. This is fine for most scenarios were only one or two clients
are connected. A future version might use asynchronous I/O with thread pooling. Also a file protocol conforming to
SuperCollider's binary OSC file format is planned.

## download

The current version can be downloaded from [github.com/Sciss/ScalaOSC](http://github.com/Sciss/ScalaOSC).
