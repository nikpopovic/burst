/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.wave.data.worker.cache

import org.burstsys.fabric.wave.data.model.limits.{FabricSnapCacheLimits, FabricSnapCacheLimitsContext}
import org.burstsys.fabric.wave.data.model.ops.FabricCacheOps
import org.burstsys.fabric.wave.data.model.slice.FabricSlice
import org.burstsys.fabric.wave.data.model.snap.{FabricSnap, FailedSnap, HotSnap, NoDataSnap}
import org.burstsys.fabric.wave.data.worker.cache.internal._
import org.burstsys.fabric.wave.data.worker.cache.lifecycle.{FabricSnapCacheBooter, FabricSnapCacheCleaner, FabricSnapCacheLoader, FabricSnapCacheTender}
import org.burstsys.fabric.wave.data.worker.pump.FabricCacheIntake
import org.burstsys.vitals.VitalsService
import org.burstsys.vitals.VitalsService.{VitalsServiceModality, VitalsSingleton}

import scala.language.postfixOps

/**
 * worker side snap cache - manages all lifecycle and OPs related to storing
 * generations (dataset instances) in the worker disk and memory.
 */
trait FabricSnapCache extends VitalsService with FabricCacheOps {

  /**
   * return a hot (in memory) [[FabricSnap]] from cache '''with a read lock'''.
   * Snap is ready for scanning.
   *
   * @return a [[HotSnap]], [[NoDataSnap]], or [[FailedSnap]]
   */
  def loadSnapWithReadLock(slice: FabricSlice): FabricSnap

  /**
   * @return current cold (in cache but not on disk) snap count
   */
  def coldSnapCount: Int

  /**
   * @return current warm (on disk, not in mem) snap count
   */
  def warmSnapCount: Int

  /**
   * @return current hot (in memory) snap count
   */
  def hotSnapCount: Int

  /**
   * @return current failed snap count
   */
  def failedSnapCount: Int

  /**
   * @return current empty snap count
   */
  def emptySnapCount: Int

  /**
   * parameter source for cache resource limits management
   *
   * @return
   */
  def limits: FabricSnapCacheLimits

  /**
   * override the default parameter source for cache resource limits management
   * (for unit tests)
   */
  def withLimits(limits: FabricSnapCacheLimits): FabricSnapCache

  /**
   * a listener for cache events
   */
  def talksTo(listener: FabricSnapCacheListener*): FabricSnapCache

}

object FabricSnapCache {

  def apply(): FabricSnapCache = FabricSnapCacheContext()

}

/**
 * cache implementation
 *
 */
private[cache] final case
class FabricSnapCacheContext() extends AnyRef
  with FabricSnapCache with FabricSnapCacheLoader with FabricSnapCacheBooter with FabricSnapCacheTender
  with FabricSnapCacheCleaner with FabricSnapCacheLocks with FabricSnapCacheTalker
  with FabricSnapCacheMap with FabricSnapCacheOps with FabricSnapCacheRegions {

  override val modality: VitalsServiceModality = VitalsSingleton

  override val serviceName: String = s"fabric-snap-cache"

  override val toString: String = serviceName

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // API
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override
  def limits: FabricSnapCacheLimits = _limits

  override
  def withLimits(limits: FabricSnapCacheLimits): FabricSnapCache = {
    _limits = limits
    this
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // protected state
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected val cache: FabricSnapCache = this

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // private state
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private[cache]
  var _limits: FabricSnapCacheLimits = FabricSnapCacheLimitsContext()

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // lifecycle
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override def start: this.type = {
    if (isRunning) {
      return this
    }
    ensureNotRunning
    markRunning
    log info startingMessage

    this talksTo FabricCacheReporter

    initializeRegionFiles()

    FabricCacheIntake.startIfNotAlreadyStarted
    internal.backgroundCleaner.startIfNotAlreadyStarted

    // boot snap cache
    bootSnapCache()

    startTender()

    talk(_.onSnapCacheStart(this))
    this
  }

  override def stop: this.type = {
    if (!isRunning) {
      return this
    }
    ensureRunning
    log info stoppingMessage
    talk(_.onSnapCacheStop(this))

    internal.backgroundCleaner.stop
    FabricCacheIntake.stop
    stopTender()

    clearCacheMap()
    clearListeners()

    markNotRunning
    this
  }

}
