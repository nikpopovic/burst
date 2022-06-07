/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.nexus

import org.burstsys.vitals.reporter.VitalsReporter
import org.burstsys.vitals.reporter.metric.VitalsReporterByteOpMetric

private[nexus]
object NexusReporter extends VitalsReporter {

  final val dName: String = "nexus"

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // private state
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private[this]
  val _transmitMetric = VitalsReporterByteOpMetric("nexus_transmit")
  this += _transmitMetric

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // LIFECYCLE
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override def sample(sampleMs: NexusSliceKey): Unit = {
    super.sample(sampleMs)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // API
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  final
  def onTransmit(ns: Long, bytes: Long): Unit = {
    newSample()
    _transmitMetric.recordOpWithTimeAndSize(ns, bytes)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // REPORT
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  final override
  def report: String = {
    if (nullData) return ""
    s"${_transmitMetric.report}"
  }

}