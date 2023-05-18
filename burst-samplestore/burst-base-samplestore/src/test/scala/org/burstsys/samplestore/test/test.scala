/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.samplestore

import org.burstsys.vitals.logging.{VitalsLog, VitalsLogger}

package object test extends VitalsLogger {
  trait BaseSpecLog {
    VitalsLog.configureLogging("base-samplestore", consoleOnly = true)
  }
}
