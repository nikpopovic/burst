/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.eql.test.expressions

import org.burstsys.eql.test.support.EqlAlloyTestRunner
import org.burstsys.fabric.wave.execution.model.result.group.FabricResultGroup
import org.burstsys.vitals.errors.VitalsException

final
class EqlMapQuerySpec extends EqlAlloyTestRunner {


  it should "successfully iterate the map keys" in {
    val source =
      s"""
         | select count(user.sessions.events) as evnts, user.sessions.events.parameters.key as kys
         | from schema Unity
       """.stripMargin

    runTest(source, 100, 200, { result =>
      checkResult(result)

      val names = result.resultSets(0).columnNames.zipWithIndex.toMap
      val r = result.resultSets(0).rowSet.map {
        row => (row(names("evnts")).asLong, row(names("kys")).asString)
      }.sortBy(_._1).sortBy(_._2)

      r should equal(Array((8928,"EK1"), (8929,"EK2"), (8929,"EK3"), (8929,"EK4"), (8929,"EK5"), (8928,"EK6"), (8928,"EK7")))
    })
  }

  it should "successfully iterate the map values" in {
    val source =
      s"""
         | select count(user.sessions.events) as evnts, user.sessions.events.parameters.value as vals
         | from schema Unity
       """.stripMargin

    runTest(source, 100, 200, { result =>
      checkResult(result)

      val names = result.resultSets(0).columnNames.zipWithIndex.toMap
      val r = result.resultSets(0).rowSet.map {
        row => (row(names("evnts")).asLong, row(names("vals")).asString)
      }.sortBy(_._1).sortBy(_._2)

      r should equal(Array((8928,"EV1"), (8929,"EV2"), (8929,"EV3"), (8929,"EV4"), (8929,"EV5"), (8928,"EV6"), (8928,"EV7")))
    })
  }

  it should "successfully do a map lookup" in {
    val source =
      s"""
         | select count(user.sessions.events) as evnts, user.sessions.events.parameters['EK3'] as vals
         | from schema Unity
       """.stripMargin

    runTest(source, 100, 200, { result =>
      checkResult(result)

      val names = result.resultSets(0).columnNames.zipWithIndex.toMap
      val r = result.resultSets(0).rowSet.map {
        row => (row(names("evnts")).asLong, row(names("vals")).asString)
      }

      r.sortBy(_._1) should equal(Array((3571, ""), (8929, "EV3")))
    })
  }

  it should "successfully do a another key map lookup (Hydra Bug)" in {
    val source =
      s"""
         |select count(user.sessions) as c, user.sessions.osVersionId as d from schema Unity
         |where user.sessions.events.parameters['EK3'] == 'EV3'
      """.stripMargin

    runTest(source, 100, 200, { result =>
      checkResult(result)

      val names = result.resultSets(0).columnNames.zipWithIndex.toMap
      val r = result.resultSets(0).rowSet.map {
        row => Array(row(names("c")).asLong, row(names("d")).asLong)
      }

      r.sortBy(_(1)) should equal(Array(Array(325, 232323), Array(300, 454545), Array(325, 676767), Array(300, 898989)))
    })
  }

  def checkResult(result: FabricResultGroup): Unit = {
    if (!result.resultStatus.isSuccess)
      throw VitalsException(s"execution failed: ${result.resultStatus}")
    if (result.groupMetrics.executionMetrics.overflowed > 0)
      throw VitalsException(s"execution overflowed")
    if (result.groupMetrics.executionMetrics.limited > 0)
      throw VitalsException(s"execution limited")
    if (result.groupMetrics.executionMetrics.rowCount <= 0)
      throw VitalsException(s"execution row count mismatch: expected rows got ${result.groupMetrics.executionMetrics.rowCount}")

    // all the besides should return a result set
    result.resultSets.keys.size should be > 0
  }
}
