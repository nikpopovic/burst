/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.hydra.test.cases.quo.inclusion

import org.burstsys.fabric.wave.execution.model.result.group.FabricResultGroup
import org.burstsys.fabric.wave.execution.model.result.row.FabricResultRow
import org.burstsys.hydra.test.cases.quo.inclusion.HydraQuoInvertedInlineSetInclusion.{assertLimits, equal, frameName}
import org.burstsys.hydra.test.cases.quo.inclusion.HydraQuoInvertedRefSetInclusion.frameName
import org.burstsys.hydra.test.cases.quo.parameters.HydraQuoParameters01.{analysisName, frameName}
import org.burstsys.hydra.test.cases.support.HydraUseCase

/**
  * =NOT WORKING YET=
  */
object HydraQuoRefSetInclusion extends HydraUseCase(1, 1, "quo") {

  override def analysisSource: String =
    s"""
       |hydra $analysisName() {
       |  schema quo
       |
       |  val gv1:set[long] = set(6049337, 4498119)
       |
       |  frame $frameName  {
       |    cube user {
       |      limit = 1
       |      cube user.sessions.events {
       |        aggregates {
       |          count:sum[long]
       |        }
       |      }
       |    }
       |    user.sessions.events => {
       |      pre => {
       |        if( user.sessions.events.eventId in gv1 ) {
       |          $analysisName.$frameName.count =  1
       |          insert($analysisName.$frameName)
       |        }
       |      }
       |    }
       |  }
       |}
     """.stripMargin

  override def
  validate(implicit result: FabricResultGroup): Unit = {
    val r = result.resultSets(result.resultSetNames(frameName))
    assertLimits(r)
    found(r.rowSet) should equal(expected)
  }


  def found(rowSet: Array[FabricResultRow]): Array[_] = {
    rowSet.map {
      row => row.cells(0).asLong
    }.sorted
  }

  val expected: Array[Any] =
    Array.empty

}
