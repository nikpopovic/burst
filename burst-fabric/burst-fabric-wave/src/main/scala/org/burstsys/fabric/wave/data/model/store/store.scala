/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.wave.data.model

import org.burstsys.fabric.wave.data.supervisor.store.FabricStoreSupervisor
import org.burstsys.fabric.wave.data.worker.store.FabricStoreWorker
import org.burstsys.vitals.VitalsService.{VitalsPojo, VitalsServiceModality}
import org.burstsys.vitals.logging._
import org.burstsys.vitals.properties.VitalsPropertyKey
import org.burstsys.vitals.{VitalsService, reflection}

import scala.jdk.CollectionConverters._

package object store extends VitalsLogger {

  type FabricStoreName = String

  /**
   * property Key to discover store choice -- goes into store properties
   */
  final val FabricStoreNameProperty: VitalsPropertyKey = "burst.store.name"

  /**
   * A supervisor or worker store implementation
   */
  trait FabricStore extends VitalsService {

    final val modality: VitalsServiceModality = VitalsPojo

    final override def serviceName: String = s"$storeName-store"

    final override def toString: String = storeName

    /**
     * @return the well known store name - used for registry/lookup
     */
    def storeName: FabricStoreName

  }

  /**
   * Fabric Store Plugin API
   */
  trait FabricStoreProvider[M <: FabricStoreSupervisor, W <: FabricStoreWorker] extends Any {

    /**
     * @return the name of this store
     */
    def storeName: FabricStoreName

    /**
     * @return The supervisor-side implementation of this store
     */
    def supervisorClass: Class[M]

    /**
     * @return The worker-side implementation of this store
     */
    def workerClass: Class[W]

  }

  private[fabric]
  lazy val storeProviders: Array[FabricStoreProvider[_, _]] = {
    reflection.getSubTypesOf(classOf[FabricStoreProvider[_, _]]).map(_.getDeclaredConstructor().newInstance()).toArray
  }

}
