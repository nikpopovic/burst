/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.samplestore.test

import org.burstsys.fabric.topology.FabricTopologyWorker
import org.burstsys.fabric.topology.supervisor.FabricTopologyListener
import org.burstsys.nexus
import org.burstsys.samplestore.store.container.{NexusConnectedPortAccessParameter, NexusHostAddrAccessParameter, NexusHostNameAccessParameter, NexusPortAccessParameter}
import org.burstsys.samplestore.store.container.supervisor.SampleStoreFabricSupervisorContainer

import java.util.concurrent.{CountDownLatch, TimeUnit}

class BaseTopologySpec extends BaseSupervisorWorkerBaseSpec
  with FabricTopologyListener {


  override protected def wantsContainers = true

  override protected def workerCount = 10

  override def configureSupervisor(supervisor: SampleStoreFabricSupervisorContainer): Unit = {
    supervisor.topology.talksTo(this)
  }

  override protected
  def beforeAll(): Unit = {
    nexus.configuration.burstNexusServerPortProperty.set(2000)
    super.beforeAll()
  }

  def latch(count: Int = workerCount): CountDownLatch = new CountDownLatch(count)

  private var workerGain = latch()
  private var workerLoss = latch()

  it should "initiate a topology" in {
    workerGain.await(30, TimeUnit.SECONDS) shouldEqual true
    supervisorContainer.topology.healthyWorkers.length should equal(workerCount)

    workerContainers.foreach(_.stop)
    Thread.sleep(100)

    workerLoss.await(15, TimeUnit.SECONDS) shouldEqual true
    supervisorContainer.topology.healthyWorkers.length should equal(0)

    workerGain = latch()
    workerContainers.foreach(_.start)
    Thread.sleep(100)

    workerGain.await(30, TimeUnit.SECONDS) shouldEqual true
    supervisorContainer.topology.healthyWorkers.length should equal(workerCount)

    workerLoss = latch(workerCount / 2)
    workerContainers.indices.foreach {
      case i if i % 2 == 0 => workerContainers(i).stop
      case _ =>
    }
    Thread.sleep(100)

    workerLoss.await(15, TimeUnit.SECONDS) shouldEqual true
    supervisorContainer.topology.healthyWorkers.length should equal(workerCount / 2)
  }

  override
  def onTopologyWorkerGained(worker: FabricTopologyWorker): Unit = {
    worker.accessParameters should not equal(null)
    worker.accessParameters.nonEmpty should equal(true)
    worker.accessParameters.keys should contain(NexusPortAccessParameter)
    worker.accessParameters(NexusPortAccessParameter) should equal(2000)
    worker.accessParameters.keys should contain(NexusConnectedPortAccessParameter)
    worker.accessParameters(NexusConnectedPortAccessParameter) should equal(2000)
    worker.accessParameters.keys should contain(NexusHostAddrAccessParameter)
    worker.accessParameters.keys should contain(NexusHostNameAccessParameter)
    workerGain.countDown()
  }

  override
  def onTopologyWorkerLoss(worker: FabricTopologyWorker): Unit = {
    workerLoss.countDown()
  }
}
