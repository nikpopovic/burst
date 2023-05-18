/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.hydra.test.cases.quo.nulls

import org.burstsys.fabric.wave.execution.model.result.group.FabricResultGroup
import org.burstsys.fabric.wave.execution.model.result.row.FabricResultRow
import org.burstsys.hydra.test.cases.quo.conditionals.HydraQuoConditionals02.{analysisName, frameName}
import org.burstsys.hydra.test.cases.support.HydraUseCase

/*
  */
object HydraQuoNulls01 extends HydraUseCase(1, 1, "quo") {


  //    override val sweep: HydraSweep = new BA8D0554E1CDE4860BFC9B66D97B443DB

  override val frameSource: String =
    s"""|frame $frameName {
        | cube user {
        |   limit = 4
        |   aggregates {
        |      'isNullcount':sum[long]
        |      'notNullcount':sum[long]
        |   }
        | }
        | user => {
        |     pre => {
        |       val lv1:boolean = user.flurryId == null
        |       val lv2:boolean = user.flurryId != null
        |       if( lv1) {
        |         $analysisName.$frameName.'isNullcount' = 1
        |       }
        |       if( lv2 )  {
        |         $analysisName.$frameName.'notNullcount' = 1
        |       }
        |       insert($analysisName.$frameName)
        |     }
        | }
        |}""".stripMargin

  override def
  validate(implicit result: FabricResultGroup): Unit = {
    val r = result.resultSets(result.resultSetNames(frameName))
    assertLimits(r)

    found(r.rowSet) should equal(expected)
  }

  def found(rowSet: Array[FabricResultRow]): Array[_] = {
    rowSet.map {
      row => (row.cells(0).asLong, row.cells(1).asLong)
    } sortBy(_._1)
  }

  val expected: Array[Any] = Array((0,26))

}
