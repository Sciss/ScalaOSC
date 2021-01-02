/*
 * BrowserDriver.scala
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

package de.sciss.osc.impl

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

object BrowserDriver {
  type Repr = js.Dictionary[BrowserEndpoint]

  def apply(): Repr = {
    val module = js.Dynamic.global.Module
    if (js.isUndefined(module.oscDriver)) {
      module.oscDriver = js.Dictionary.empty[BrowserEndpoint]
    }
    module.oscDriver.asInstanceOf[Repr]
  }
}

// @js.native
trait BrowserEndpoint extends js.Object {
  def receive: js.Function2[Int, Uint8Array, Unit]
}