/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.execution.worker

import org.burstsys.fabric.container.FabricWorkerService
import org.burstsys.fabric.container.worker.FabricWorkerContainer
import org.burstsys.fabric.data
import org.burstsys.fabric.data.model.snap.{FabricSnap, FailedSnap, HotSnap, NoDataSnap}
import org.burstsys.fabric.execution.model.gather.FabricGather
import org.burstsys.fabric.execution.model.gather.control.FabricFaultGather
import org.burstsys.fabric.execution.model.gather.data.FabricDataGather
import org.burstsys.fabric.execution.model.pipeline.publishPipelineEvent
import org.burstsys.fabric.execution.model.wave.FabricParticle
import org.burstsys.fabric.net.client.connection.{ParticleExecutionDataReady, ParticleExecutionFinished, ParticleExecutionStart}
import org.burstsys.fabric.trek.{FabricWorkerFetchTrekMark, FabricWorkerScanTrekMark}
import org.burstsys.vitals.VitalsService.{VitalsServiceModality, VitalsStandardServer}
import org.burstsys.vitals.errors._
import org.burstsys.vitals.healthcheck.VitalsHealthMonitoredService
import org.burstsys.vitals.logging._
import org.burstsys.vitals.uid.VitalsUid

/**
 * Client (worker) side execution
 */
trait FabricWorkerEngine extends FabricWorkerService {

  /**
   * execute one worker's part of a wave operation
   *
   * @param particle
   */
  def executionParticleOp(ruid: VitalsUid, particle: FabricParticle): FabricGather

}

object FabricWorkerEngine {
  def apply(container: FabricWorkerContainer, mode: VitalsServiceModality = VitalsStandardServer): FabricWorkerEngine =
    FabricWorkerContextEngine(container, mode: VitalsServiceModality)
}

private final case
class FabricWorkerContextEngine(container: FabricWorkerContainer, modality: VitalsServiceModality)
  extends FabricWorkerEngine with VitalsHealthMonitoredService {

  override def serviceName: String = s"fabric-worker-engine"

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // API
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override
  def executionParticleOp(ruid: VitalsUid, particle: FabricParticle): FabricGather =
    sliceScan(particle, sliceFetch(particle))

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // INTERNAL
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * the data fetch part of the particle op
   *
   * @param particle
   * @return
   */
  private def sliceFetch(particle: FabricParticle): FabricSnap = {
    val tag = s"FabricWorkerEngine.sliceFetch(guid=${particle.guid})"
    log info burstStdMsg(s"$tag start")
    try {
      val start = System.nanoTime
      FabricWorkerFetchTrekMark.begin(particle.slice.guid)
      publishPipelineEvent(ParticleExecutionStart(particle.slice.guid))

      val snap = data.worker.cache.instance.loadSnapWithReadLock(particle.slice)

      snap.state match {

        case HotSnap | NoDataSnap =>
          FabricEngineReporter.snapFetch(elapsedNs = System.nanoTime - start)
          FabricWorkerFetchTrekMark.end(particle.slice.guid)
          publishPipelineEvent(ParticleExecutionDataReady(particle.slice.guid))

        case FailedSnap => throw snap.lastFail.get

        case _ => throw VitalsException(s"unexpected state for fetched snap ${snap.state}")
      }

      snap
    } catch safely {
      case t: Throwable =>
        FabricWorkerFetchTrekMark.fail(particle.slice.guid)
        log error burstStdMsg(s"FAB_SLICE_FETCH_FAIL $t $tag", t)
        throw t
    }
  }

  /**
   * the data scan part of the particle op
   *
   * @param particle
   * @param snap
   * @return
   */
  private
  def sliceScan(particle: FabricParticle, snap: FabricSnap): FabricGather = {
    val tag = s"FabricWorkerEngine.sliceScan(snap=${snap.guid}, guid=${particle.guid})"
    log info s"FAB_SLICE_SCAN_START $tag"
    val start = System.nanoTime
    FabricWorkerScanTrekMark.begin(particle.slice.guid)
    val scanner = particle.scanner
    try {
      scanner.beforeAllScans(snap)
      val result = scanner.scanMergeRegionsInSlice(snap.data.iterators) match {
        case gather: FabricFaultGather => throw gather.fault // throw to outer catch
        case gather: FabricDataGather =>
          log info s"FAB_SLICE_SCAN_SUCCESS $tag"
          FabricEngineReporter.successfulScan(elapsedNs = System.nanoTime - start, gather)
          FabricWorkerScanTrekMark.end(particle.slice.guid)
          publishPipelineEvent(ParticleExecutionFinished(particle.slice.guid))
          gather
      }
      result
    } catch safely {
      case t: Throwable =>
        log error burstStdMsg(s"FAB_SLICE_SCAN_FAIL ${t} $tag", t)
        publishPipelineEvent(ParticleExecutionFinished(particle.slice.guid))
        FabricWorkerScanTrekMark.fail(particle.slice.guid)
        FabricEngineReporter.failedScan()
        FabricFaultGather(particle.scanner, t)
    } finally {
      scanner.afterAllScans(snap)
      snap.releaseSnapReadLock()
    }

  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Lifecycle
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override
  def start: this.type = {
    synchronized {
      ensureNotRunning
      log info startingMessage
      markRunning
      this
    }
  }

  override
  def stop: this.type = {
    synchronized {
      ensureRunning
      log info stoppingMessage
      markNotRunning
      this
    }
  }

}
