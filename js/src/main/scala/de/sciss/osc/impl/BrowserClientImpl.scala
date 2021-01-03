/*
 * BrowserClientImpl.scala
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

private[osc] final class BrowserClientImpl(protected val target: Browser.Address,
                                           protected val config: Browser.Config)
  extends ClientImpl[Browser.Address] with BrowserChannelImpl {

  protected val input : Browser.Receiver   .Directed = Browser.Receiver   (target, config)
  protected val output: Browser.Transmitter.Directed = Browser.Transmitter(target, config)
}
