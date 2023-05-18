/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.tesla.test.reporter

import org.burstsys.tesla.block.TeslaBlockReporter
import org.burstsys.tesla.test.support.TeslaSpecLog
import org.burstsys.vitals.reporter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Ignore, Suite}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

@Ignore
class TeslaMemoryReportSpec extends AnyFlatSpec with Suite with Matchers with BeforeAndAfterAll with TeslaSpecLog {

  it should "execution report" in {
    reporter.startReporterSystem(samplePeriod = 1 second, reportPeriod = 1 second, waitPeriod = 1 second)

    for (_ <- 0 until 10) {
      val reportCount = Math.abs(Random.nextDouble() * 30).toInt
      for (_ <- 0 until reportCount) {
        val free = Math.abs(Random.nextDouble() * 1e6).toInt
        if (Random.nextBoolean())
          TeslaBlockReporter.grab()
        else
          TeslaBlockReporter.free(free)
        Thread.sleep(100)
      }
      Thread.sleep(100)
    }

  }

}
