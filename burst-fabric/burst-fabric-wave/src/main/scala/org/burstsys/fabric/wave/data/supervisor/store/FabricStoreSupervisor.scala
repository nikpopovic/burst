/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.wave.data.supervisor.store

import org.burstsys.fabric.wave.data.model.slice.FabricSlice
import org.burstsys.fabric.wave.data.model.store.FabricStore
import org.burstsys.fabric.wave.metadata.model.datasource.FabricDatasource
import org.burstsys.fabric.topology.model.node.worker.FabricWorkerNode
import org.burstsys.vitals.uid._

import scala.concurrent.Future

/**
 * supervisor side processing and management for data stores
 */
trait FabricStoreSupervisor extends FabricStore {

  /**
   * the primary routine that a worker store is required to implement
   *
   * @param workers a set of candidate workers
   * @return
   */
  def slices(guid: VitalsUid, workers: Array[FabricWorkerNode], datasource: FabricDatasource): Future[Array[FabricSlice]]

}
