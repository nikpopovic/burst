/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.brio.test

import org.burstsys.brio
import org.burstsys.vitals.logging._
import org.apache.logging.log4j.Logger
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class BrioAbstractSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  VitalsLog.configureLogging("brio", true)

  brio.provider.loadBrioSchemaProviders()

}
