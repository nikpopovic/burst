/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.zap.cube2

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import org.burstsys.brio.dictionary.flex.BrioFlexDictionary
import org.burstsys.brio.dictionary.mutable.BrioMutableDictionary
import org.burstsys.brio.types.BrioPrimitives.BrioPrimitive
import org.burstsys.felt.model.collectors.cube.{FeltCubeBuilder, FeltCubeCollector}
import org.burstsys.tesla.TeslaTypes.{TeslaMemoryOffset, TeslaMemoryPtr, TeslaMemorySize, TeslaNullMemoryPtr}
import org.burstsys.tesla.block.TeslaBlockPart
import org.burstsys.tesla.flex.TeslaFlexCollector
import org.burstsys.vitals.bitmap.VitalsBitMapAnyVal
import org.burstsys.vitals.errors.VitalsException
import org.burstsys.zap.cube2
import org.burstsys.zap.cube2.algorithms.{ZapCube2Join, ZapCube2Merge, ZapCube2Truncate}
import org.burstsys.zap.cube2.key.ZapCube2Key
import org.burstsys.zap.cube2.row.ZapCube2Row
import org.burstsys.zap.cube2.state._

/**
 * =Gen-2 Zap Cubes=
 * <hr/>
 * <ol>
 * <li>Cubes are a special FeltCollector that is used to
 * capture results during analysis scans and uniquely return them as tabular ''result-sets'' to the
 * the analysis API. Currently this is the only way that result sets are returned though
 * it is intended to add object-tree results sets as an alternative using
 * [[org.burstsys.zap.shrub.ZapShrub]] collectors some day</li>
 * <li>Cubes are '''tabular''' structures consisting of zero or more '''rows''' where
 * each row consists of a fixed set of '''columns'''</li>
 * <li>each column is either a '''dimension''' or an '''aggregation'''</li>
 * <li>dimension columns can be thought of as a composite key that creates a row's '''identity'''
 * i.e. a ``group-by`` </li>
 * <li>aggregation columns are some sort of '''calculation''' or '''count''' associated
 * with the row's dimension</li>
 * <li>each dimension and aggregation column has a '''semantic''' which defines the behavior
 * of that column for
 * the lifetime of the use of the cube</li>
 * <li>the '''shared''' (across all cubes used in a single query) ''immutable metadata object''
 * that contains the
 * definition for the fixed set of aggregation and dimension columns along with their types
 * and semantics is
 * defined in the [[org.burstsys.zap.cube2.ZapCube2Builder]] type. This shared instance
 * must be provided as the cube
 * API is accessed because a pointer to this object is '''not''' stored in the cube itself.</li>
 * <li>Currently there is a numeric limit of MaxInt / 2 rows (2 Billion or 2,147,483,648). Clearly that is impractical for other reasons</li>
 * <li>We allow nulls for dimensions and aggregations. The latter semantic is not yet completely nailed down since we start aggregations at zero</li>
 * <li>the basic cube '''update''' is performed by first '''navigating''' to the specific row
 * by setting the dimension column values and then updating one or more aggregation columns</li>
 * <li>after navigation, either an existing row is selected or a new one is created and then selected</li>
 * <li>Cubes have various '''algorithms''' which can be applied to their row sets
 * e.g. ``merge`` ([[org.burstsys.zap.cube2.algorithms.ZapCube2Merge]]),
 * ``join``  ([[org.burstsys.zap.cube2.algorithms.ZapCube2Join]]),
 * ``truncate``  ([[org.burstsys.zap.cube2.algorithms.ZapCube2Truncate]])</li>
 * <li>This structure is meant to be cached and re-used via the reset method.</li>
 * <li>We support a maximum of __64__ total dimensions and aggregations though we could eventually support more.</li>
 * <li>There is a finite limit to the number of rows but currently it seems absurdly high so not to worry.</li>
 * <li>For simplicity, everything is a long including dimensions, aggregations, pointers, null maps.</li>
 * <li>Cubes are implemented using off-heap native process memory and carefully stored in
 * [[org.burstsys.tesla.part.TeslaPartPool]] instances. This is to reduce garbage
 * collector pressure (object churn),
 * to keep memory references as local as possible, and to encourage the
 * use of ''compiler-intrinsic'' bytecode</li>
 * <li>cubes '''cannot''' be considered to be ``thread safe`` (reentrant) and it is assumed that all individual scans are performed
 * by a single thread/core</li>
 * </ol>
 */
trait ZapCube2 extends Any with FeltCubeCollector with ZapCube2DimensionAxis
  with ZapCube2AggregationAxis with TeslaBlockPart with TeslaFlexCollector[ZapCube2Builder, ZapCube2] {

  def clear(): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Dirty Rows
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def resetDirtyRows(): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Dictionary
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * access the internal flex dictionary
   */
  def dictionary: BrioFlexDictionary

  /**
   * set the internal flex dictionary
   */
  def dictionary_=(d: BrioMutableDictionary): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Buckets
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * number of buckets in this cube. This value is fixed at cube allocation time in the [[ZapCube2Builder]]
   * object.
   */
  def bucketsCount: Int


  /**
   * the offset of the first row in the indexed bucket list or EmptyBucket is this indexed bucket is empty.
   * This value is the offset of the first row in the bucket list from the cube's '''basePtr'''. This value
   * is [[ZapCube2EmptyBucket]] if the bucket is empty.
   */
  def bucketRead(index: Int): TeslaMemoryOffset

  /**
   * calculate the std deviation of bucket distribution
   *
   * @return
   */
  def bucketStdDeviation: Double

  /**
   * calculate the longest bucket list
   *
   * @return
   */
  def bucketListLengthMax: Int

  /**
   * how many times this cube was resized
   *
   * @return
   */
  def resizeCount: Int

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Dimensions
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * write a faux dimension to create a row where there are only aggregations
   */
  def dimWrite(): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Rows
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * number of rows in this cube
   */
  def rowsCount: Int

  /**
   * did we reach the current row limit?
   */
  def rowsLimited: Boolean

  /**
   * return a row by index
   */
  def row(index: Int): ZapCube2Row

  /**
   * are there no rows?
   */
  def isEmpty: Boolean

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Cursor
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * the cursor for this cube
   */
  def cursor: ZapCube2Key

  /**
   * return the offset of the row the cursor points at
   */
  def cursorRow: TeslaMemoryOffset

  /**
   * set the cursor from the dimensions of a row
   */
  def setCursorFrom(row: ZapCube2Row): Unit

  /**
   * clear the cursor for re-use
   */
  def resetCursor(): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Navigation
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * navigate to an new key coordinate
   */
  def navigate(key: ZapCube2Key): ZapCube2Row

  /**
   * navigate to the current cursor coordinate
   */
  def navigate(): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Pivot
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * get the cube's join pivot key - used as a tmp for join operaitons
   */
  def pivot: ZapCube2Key

  /**
   * clear the pivot so it can be re-used
   */
  def resetPivot(): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // CODEC
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def write(k: Kryo, out: Output): Unit

  def read(k: Kryo, in: Input): Unit

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // internal usage
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * * create a row in the result cube that has  the active dimension and aggregation columns from the from
   * * the parent and the active dimension/aggregations from the child mask.
   */
  def createJoinRow(parentRow: ZapCube2Row,
                    childRow: ZapCube2Row,
                    resultCube: ZapCube2,
                    parentDimensionMask: VitalsBitMapAnyVal, parentAggregationMask: VitalsBitMapAnyVal,
                    childDimensionMask: VitalsBitMapAnyVal, childAggregationMask: VitalsBitMapAnyVal
                   ): ZapCube2Row

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // algorithms
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def validateRow(row: ZapCube2Row): Boolean
}

/**
 */
final case
class ZapCube2AnyVal(blockPtr: TeslaMemoryPtr = TeslaNullMemoryPtr) extends AnyVal with ZapCube2 with ZapCube2State
  with ZapCube2Codec with ZapCube2Nav with ZapCube2Print with ZapCube2Iterator
  with ZapCube2Join with ZapCube2Truncate with ZapCube2Merge with ZapCube2Extract {

  override def size(): TeslaMemorySize = availableMemorySize

  override
  def importCollector(sourceCollector: ZapCube2, sourceItems: Int, builder: ZapCube2Builder): Unit =
    importCube(source = sourceCollector.asInstanceOf[ZapCube2AnyVal], rows = sourceItems)

  override
  def defaultBuilder: ZapCube2Builder = throw VitalsException(s"default builder not allowed")

  override
  def builder: ZapCube2Builder =
    cube2.ZapCube2Builder(dimensionCount = dimCount, aggregationCount = aggCount)

  override
  def itemLimited: Boolean = rowsLimited

  override
  def itemLimited_=(s: Boolean): Unit = rowsLimited = s

  override
  def itemCount: Int = rowsCount

  override
  def itemCount_=(count: Int): Unit = rowsCount = count

  override
  def inheritCursor(builder: FeltCubeBuilder, thisCube: FeltCubeCollector, parentCube: FeltCubeCollector): FeltCubeCollector = {
    inheritCursorFrom(parentCube.asInstanceOf[ZapCube2])
    this
  }

  override
  def writeAggregationNull(builder: FeltCubeBuilder, thisCube: FeltCubeCollector, column: Int): Unit = {
    aggSetNull(column)
  }

  override
  def readAggregationPrimitive(builder: FeltCubeBuilder, thisCube: FeltCubeCollector, aggregation: Int): BrioPrimitive = {
    aggRead(aggregation)
  }

  override
  def writeAggregationPrimitive(builder: FeltCubeBuilder, thisCube: FeltCubeCollector, aggregation: Int, value: BrioPrimitive): Unit = {
    aggWrite(aggregation, value)
  }

  override
  def readAggregationNull(builder: FeltCubeBuilder, thisCube: FeltCubeCollector, aggregation: Int): Boolean = {
    aggIsNull(aggregation)
  }

  override
  def writeDimension(builder: FeltCubeBuilder, thisCube: FeltCubeCollector): Unit = {
    dimWrite()
  }

  override
  def writeDimensionNull(builder: FeltCubeBuilder, thisCube: FeltCubeCollector, dimension: Int): Unit = {
    dimSetNull(dimension)
  }

  override
  def writeDimensionPrimitive(builder: FeltCubeBuilder, thisCube: FeltCubeCollector, dimension: Int, value: BrioPrimitive): Unit = {
    dimWrite(dimension, value)
  }
}
