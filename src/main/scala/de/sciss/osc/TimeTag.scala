/*
 * Timetag.scala
 * (ScalaOSC)
 *
 * Copyright (c) 2008-2018 Hanns Holger Rutz. All rights reserved.
 *
 * This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.osc

import java.util.Locale
import java.text.{DecimalFormat, NumberFormat, SimpleDateFormat}

object TimeTag {
  /** The special time-tag value
    * to indicate that the bundle be
    * processed as soon as possible
    */
  val now = TimeTag(1)

  /** Converts a relative time value in seconds, as required
    * for example for scsynth offline rendering, into a raw time-tag.
    */
  def secs(delta: Double): TimeTag = TimeTag((delta.toLong << 32) + ((delta % 1.0) * 0x100000000L + 0.5).toLong)

  private val datef = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
  private val decimf: NumberFormat = {
    val res = NumberFormat.getInstance(Locale.US)
    res match {
      case d: DecimalFormat =>
        d.setGroupingUsed(false)
        d.setMinimumFractionDigits(1)
        d.setMaximumFractionDigits(5)
      case _ =>
    }
    res
  }

  /** Converts a time value from the system clock value in milliseconds since
    * jan 1 1970, as returned by System.currentTimeMillis, into a raw time-tag.
    */
  def millis(abs: Long): TimeTag = {
    val secsSince1900  =    abs / 1000 + SECONDS_FROM_1900_TO_1970
    val secsFractional = (((abs % 1000) << 32) + 500) / 1000
    TimeTag((secsSince1900 << 32) | secsFractional)
  }

  private[osc] val SECONDS_FROM_1900_TO_1970 = 2208988800L
}

final case class TimeTag(raw: Long) {
  /** Converts the raw time-tag into a time value from the system clock value in milliseconds since
    * jan 1 1970, corresponding to what is returned by System.currentTimeMillis.
    */
  def toMillis: Long = {
    val m1 = ((raw & 0xFFFFFFFFL) * 1000) >> 32
    val m2 = (((raw >> 32) & 0xFFFFFFFFL) - TimeTag.SECONDS_FROM_1900_TO_1970) * 1000
    m1 + m2
  }

  /** Converts a raw time-tag into a relative time value in seconds, as required
    * for example for scsynth offline rendering. In general, this will return
    * the amount of seconds since midnight on January 1, 1900, as defined by
    * the OSC standard.
    */
  def toSecs: Double = {
    val frac = (raw & 0xFFFFFFFFL).toDouble / 0x100000000L
    val secs = (raw >> 32).toDouble
    secs + frac
  }

  override def toString: String = {
    if (raw == 1) "<now>"
    else {
      val secsSince1900 = (raw >> 32) & 0xFFFFFFFFL
      if (secsSince1900 > TimeTag.SECONDS_FROM_1900_TO_1970) {
        TimeTag.datef.format(toMillis)
      } else {
        TimeTag.decimf.format(toSecs)
      }
    }
  }
}