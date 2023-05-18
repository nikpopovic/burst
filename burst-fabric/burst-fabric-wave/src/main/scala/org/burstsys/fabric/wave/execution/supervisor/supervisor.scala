/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.wave.execution

import org.burstsys.fabric.wave.execution.model.gather.FabricGather
import org.burstsys.fabric.topology.model.node.worker.FabricWorkerProxy
import org.burstsys.tesla.scatter.TeslaScatterRequestContext
import org.burstsys.vitals.logging._

package object supervisor extends VitalsLogger {

  abstract class FabricScatteredGatherRequest extends TeslaScatterRequestContext[FabricGather] {

    /**
     * the worker this request is being executed on
     */
    def worker: FabricWorkerProxy

  }

}
