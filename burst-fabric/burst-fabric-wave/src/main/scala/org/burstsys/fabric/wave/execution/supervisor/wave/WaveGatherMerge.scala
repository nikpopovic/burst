/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.wave.execution.supervisor.wave

import org.burstsys.fabric.wave.execution.model.gather.FabricGather
import org.burstsys.fabric.wave.execution.model.gather.data.FabricDataGather
import org.burstsys.fabric.wave.execution.model.gather.data.FabricEmptyGather
import org.burstsys.fabric.wave.execution.model.gather.metrics.FabricGatherMetrics
import org.burstsys.fabric.wave.execution.worker.ResultQueue
import org.burstsys.tesla.scatter.TeslaScatter
import org.burstsys.tesla.thread.request.TeslaRequestFuture
import org.burstsys.tesla.thread.worker.TeslaWorkerCoupler
import org.burstsys.vitals.errors.safely
import org.burstsys.vitals.logging._
import org.burstsys.vitals.uid.VitalsUid

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.Promise

/**
 * stores all the state associated with a single wave on the supervisor
 */
trait WaveGatherMerge extends Any {

  /**
   * @return global operation UID
   */
  def guid: VitalsUid

  /**
   * @return a future with the result of the merge
   */
  def mergedResult: Future[FabricGather]

  /**
   * put a gather onto the merge processing queue
   */
  def mergeGather(gather: FabricGather): Unit

  /**
   * put  [[EndOfQueueGather]] on merge pipeline
   */
  def shutdownMerge(): Unit

}


object WaveGatherMerge {

  def apply(scatter: TeslaScatter): WaveGatherMerge = WaveGatherMergeContext(scatter)

}

private final case
class WaveGatherMergeContext(scatter: TeslaScatter) extends WaveGatherMerge {

  //////////////////////////////////////////////////////////////////////////////
  // PRIVATE STATE
  //////////////////////////////////////////////////////////////////////////////

  private val _startNanos = System.nanoTime

  private val _totalParticles = scatter.activeSlots

  private val _workQueue = new ResultQueue(_totalParticles + 2)

  private var _processedParticles = 0

  private var _result: FabricGather = _

  private val _sliceMetrics: ArrayBuffer[FabricGatherMetrics] = new ArrayBuffer[FabricGatherMetrics]

  private val _promise = Promise[FabricGather]()

  // this variable is ever accessed, but that's ok
  private val _mergeWorker: Future[Unit] = TeslaRequestFuture {
    var next = _workQueue.take()
    while (next != EndOfQueueGather) {
      TeslaWorkerCoupler(processResult(next))
      next = _workQueue.take()
    }
    TeslaWorkerCoupler(finalizeMerge())
    _promise.success(_result)
  }

  //////////////////////////////////////////////////////////////////////////////
  // API
  //////////////////////////////////////////////////////////////////////////////

  override val guid: VitalsUid = scatter.guid

  override def mergedResult: Future[FabricGather] = _promise.future

  override def mergeGather(gather: FabricGather): Unit = {
    _workQueue.put(gather)
    _processedParticles += 1
  }

  override def shutdownMerge(): Unit = {
    _workQueue.put(EndOfQueueGather)
  }

  private def processResult(nextGather: FabricGather): Unit = {
    lazy val tag = s"FabricWaveState.nextParticleResult(guid=$guid)"
    try {
      _sliceMetrics += nextGather.gatherMetrics
      _result match {
        case _: FabricEmptyGather | null =>
          _result = nextGather
        case _ =>
          _result.waveMerge(nextGather)
      }
    } catch safely {
      case t: Throwable =>
        log error burstStdMsg(s"FAB_WAVE_STATE_FAULT $t $tag ", t)
        _promise.failure(t)
        throw t
    }
  }

  private def finalizeMerge(): Unit = {
    lazy val tag = s"FabricWaveState.finalizeMerge(guid=$guid)"
    try {
      if (_result == null) {
        log warn s"FAB_WAVE_STATE_NO_RESULT $tag"
        return
      }
      _result.waveFinalize()
      _result match {
        case gather: FabricDataGather =>
          _result.gatherMetrics.executionMetrics.recordFinalMetricsOnSupervisor(gather)
        case _ =>
      }
      _result.gatherMetrics.finalizeWaveMetricsOnSupervisor(_sliceMetrics.toArray)
    } catch safely {
      case t: Throwable =>
        log error burstStdMsg(s"FAB_WAVE_STATE_FAULT $t $tag", t)
        _promise.failure(t)
        throw t
    }
  }

}
