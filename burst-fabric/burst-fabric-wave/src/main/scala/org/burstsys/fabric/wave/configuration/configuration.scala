/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.wave

import org.apache.commons.io.FileUtils
import org.burstsys.fabric.wave.metadata.{ViewCacheEraseTtlMsProperty, ViewCacheEvictTtlMsProperty, ViewCacheFlushTtlMsProperty}
import org.burstsys.fabric.wave.metadata.model.datasource.FabricDatasource
import org.burstsys.tesla.configuration.burstTeslaWorkerThreadCountProperty
import org.burstsys.vitals.stats.GB
import org.burstsys.vitals.net.{VitalsHostAddress, VitalsHostPort, getPublicHostAddress, getPublicHostName}
import org.burstsys.vitals.properties._

import java.lang.Runtime.getRuntime
import java.nio.file.Paths
import scala.concurrent.duration._
import scala.language.postfixOps

package object configuration extends VitalsPropertyRegistry {

  def configureForUnitTests(): Unit = {
    burstFabricCacheImpellersProperty.set(2)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // THREADING/CONCURRENCY LIMITS
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * maximum number of concurrent waves allowed through the supervisor
   */
  val burstFabricWaveConcurrencyProperty: VitalsPropertySpecification[Int] = VitalsPropertySpecification[Int](
    key = "burst.fabric.wave.concurrency",
    description = "max allowed concurrent waves",
    default = Some(12) // for now...
  )

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // REGIONS
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * a '';'' separated list of spindle data folders
   */
  val burstFabricCacheSpindleFoldersProperty: VitalsPropertySpecification[String] = VitalsPropertySpecification[String](
    key = "burst.fabric.cache.spindles",
    description = "a ';' separated list of spindle dirs",
    default = Some(cacheRegionTmpFolders.mkString(";"))
  )

  def cacheSpindleFolders: Array[String] = {
    burstFabricCacheSpindleFoldersProperty.get.split(";")
  }

  /**
   * The number of regions in a slice. In a slice each region is backed by a file. By default we set the number of
   * of regions equal to the number of worker threads, so that if a worker is scanning a slice it can use all available
   * worker threads to share in the processing. In the future this may be configurable, but it requires more testing
   * to verify what values would be reasonable.
   */
  val burstFabricCacheRegionCount: Int = burstTeslaWorkerThreadCountProperty.get

  /**
   * the number of impellers per spindle. Basically this is how hard you want to hit disk IO on a single
   * physical disk's queue
   */
  val burstFabricCacheImpellersProperty: VitalsPropertySpecification[Int] = VitalsPropertySpecification[Int](
    key = "burst.fabric.cache.impellers",
    description = "how many impellers per spindle",
    default = Some(8)
  )

  /**
   * tmp folder version of cache for unit tests
   *
   * @return
   */
  private def cacheRegionTmpFolders: Array[String] = {
    (0 until 4).map {
      i =>
        val tmpFolder = Paths.get(FileUtils.getTempDirectoryPath, s"burst-cache$i")
        val tmpFile = tmpFolder.toAbsolutePath.toFile
        FileUtils.forceMkdir(tmpFile)
        FileUtils.cleanDirectory(tmpFile)
        tmpFolder.toAbsolutePath.toString

    }.toArray
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // CACHE EVICT/FLUSH
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * flush (delete) all snap cache files when a worker boots
   */
  val burstFabricCacheBootFlushProperty: VitalsPropertySpecification[Boolean] = VitalsPropertySpecification[Boolean](
    key = "burst.fabric.cache.boot.flush",
    description = "do a boot flush of all slice generations",
    default = Some(true)
  )

  val burstFabricCacheTendMinutesProperty: VitalsPropertySpecification[Long] = VitalsPropertySpecification[Long](
    key = "burst.fabric.cache.tend.period.minutes",
    description = "how often to tend the cache",
    default = Some((5 minutes).toMinutes)
  )

  /**
   * percentage of memory used where evictions starts
   */
  val burstFabricCacheMemoryHighMarkPercentProperty: VitalsPropertySpecification[Double] = VitalsPropertySpecification[Double](
    key = "burst.fabric.cache.memory.high.percent",
    description = "high water mark memory usage percentage",
    default = Some(40.0)
  )

  /**
   * percentage of memory used where evictions stops
   */
  val burstFabricCacheMemoryLowMarkPercentProperty: VitalsPropertySpecification[Double] = VitalsPropertySpecification[Double](
    key = "burst.fabric.cache.memory.low.percent",
    description = "low water mark memory usage percentage",
    default = Some(25.0)
  )

  /**
   * percentage of disk used where flushing starts
   */
  val burstFabricCacheDiskHighMarkPercentProperty: VitalsPropertySpecification[Double] = VitalsPropertySpecification[Double](
    key = "burst.fabric.cache.disk.high.percent",
    description = "high water mark disk usage percentage",
    default = Some(60.0)
  )

  /**
   * percentage of disk used where flushing stops
   */
  val burstFabricCacheDiskLowMarkPercentProperty: VitalsPropertySpecification[Double] = VitalsPropertySpecification[Double](
    key = "burst.fabric.cache.disk.low.percent",
    description = "low water mark disk usage percentage",
    default = Some(50.0)
  )

  val burstViewCacheFaultHealProperty: VitalsPropertySpecification[Duration] = VitalsPropertySpecification[Duration](
    key = "burst.fabric.cache.fault.heal.duration",
    description = "how long before a dataset should attempt a reload from the remote store",
    default = Some(10.seconds)
  )

  /**
   * @see [[org.burstsys.fabric.metadata.ViewCacheEvictTtlMsProperty]]
   *      how many ms before a cached dataset is considered for eviction
   */
  val burstViewCacheEvictTtlMsPropertyDefault: VitalsPropertySpecification[Long] = VitalsPropertySpecification[Long](
    key = ViewCacheEvictTtlMsProperty,
    description = "ms after last access cache is triggered to evict (after read/write)",
    default = Some((15 minutes).toMillis)
  )

  /**
   * @see [[org.burstsys.fabric.metadata.ViewCacheFlushTtlMsProperty]]
   *      how many ms before a cached dataset is considered for flushing
   */
  val burstViewCacheFlushTtlMsPropertyDefault: VitalsPropertySpecification[Long] = VitalsPropertySpecification[Long](
    key = ViewCacheFlushTtlMsProperty,
    description = "ms after last access cache is triggered to flush (after evict)",
    default = Some((2 hours).toMillis)
  )

  /**
   * @see [[org.burstsys.fabric.metadata.ViewCacheEraseTtlMsProperty]]
   *      how many ms before a cached dataset is considered for eraseing
   */
  val burstViewCacheEraseTtlMsPropertyDefault: VitalsPropertySpecification[Long] = VitalsPropertySpecification[Long](
    key = ViewCacheEraseTtlMsProperty,
    description = "ms after last access cache is triggered to erase (after flush)",
    default = Some((30 minutes).toMillis)
  )

  final def evictTtlMsFromDatasource(datasource: FabricDatasource): Long =
    datasource.view.viewProperties.extend.getValueOrDefault(
      metadata.ViewCacheEvictTtlMsProperty, burstViewCacheEvictTtlMsPropertyDefault.get
    )

  final def flushTtlMsFromDatasource(datasource: FabricDatasource): Long =
    datasource.view.viewProperties.extend.getValueOrDefault(
      metadata.ViewCacheFlushTtlMsProperty, burstViewCacheFlushTtlMsPropertyDefault.get
    )

  final def eraseTtlMsFromDatasource(datasource: FabricDatasource): Long =
    datasource.view.viewProperties.extend.getValueOrDefault(
      metadata.ViewCacheEraseTtlMsProperty, burstViewCacheEraseTtlMsPropertyDefault.get
    )

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Data sources
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  val burstFabricDatasourceMaxSizeProperty: VitalsPropertySpecification[Long] = VitalsPropertySpecification[Long](
    key = "burst.fabric.datasource.max.size.bytes",
    description = "the maximum desired size of a loaded dataset. Datasets that exceed this size will have the `loadInvalid` flag set in their generation metrics",
    default = Some(100 * GB)
  )

  final def maxDatasetSizeFromDatasource(datasource: FabricDatasource): Long =
    datasource.view.viewProperties.extend.getValueOrDefault(
      metadata.ViewNextDatasetSizeMaxProperty, burstFabricDatasourceMaxSizeProperty.get
    )

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Cache loops
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  val burstFabricCacheLoadLoopWaitQuantum: VitalsPropertySpecification[Duration] = VitalsPropertySpecification[Duration](
    key = "burst.fabric.cache.load.loop.step",
    description = "how long the load loop waits to acquire a lock",
    default = Some(1.second)
  )

  val burstFabricCacheLoadLoopMaxWait: VitalsPropertySpecification[Duration] = VitalsPropertySpecification[Duration](
    key = "burst.fabric.cache.load.loop.max",
    description = "how long the load loop tries to acquire a lock before aborting",
    default = Some(5.minutes)
  )

  val burstFabricCacheTenderLoopWaitQuantum: VitalsPropertySpecification[Duration] = VitalsPropertySpecification[Duration](
    key = "burst.fabric.cache.tender.loop.step",
    description = "how long the tender loop waits to acquire a lock",
    default = Some(10.seconds)
  )

  val burstFabricCacheTenderLoopMaxWait: VitalsPropertySpecification[Duration] = VitalsPropertySpecification[Duration](
    key = "burst.fabric.cache.tender.loop.max",
    description = "how long the tender loop tries to acquire a lock before aborting",
    default = Some(1.minute)
  )

}
