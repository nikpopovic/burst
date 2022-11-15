/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.alloy.alloy.usecase

import org.burstsys.fabric.topology.FabricTopologyWorker
import org.burstsys.fabric.topology.supervisor.FabricTopologyListener
import org.burstsys.fabric.wave.container.supervisor.MockWaveSupervisorContainer
import org.burstsys.fabric.wave.container.worker.MockWaveWorkerContainer
import org.burstsys.fabric.wave.data.worker.cache
import org.burstsys.tesla.part.factory.TeslaFactoryBoss
import org.burstsys.vitals.configuration.burstVitalsHealthCheckPortProperty
import org.burstsys.vitals.errors.safely
import org.burstsys.vitals.logging._
import org.burstsys.{fabric, tesla, vitals}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Suite}

import java.util.concurrent.CountDownLatch
import scala.language.postfixOps

abstract class AlloyJsonUseCaseRunner extends AnyFlatSpec
  with Suite with Matchers with BeforeAndAfterAll with FabricTopologyListener with AlloyUnitMetadataLookup {

  VitalsLog.configureLogging("unit", consoleOnly = true)
  vitals.configuration.configureForUnitTests()
  tesla.configuration.configureForUnitTests()
  fabric.wave.configuration.configureForUnitTests()

  val supervisorContainer: MockWaveSupervisorContainer = MockWaveSupervisorContainer(logFile = "unit", containerId = 1)
  protected var workerContainer: MockWaveWorkerContainer = {
    // we mix supervisor and worker in the same JVM so move the health port
    val port = burstVitalsHealthCheckPortProperty.get
    burstVitalsHealthCheckPortProperty.set(port + 1)
    MockWaveWorkerContainer(logFile = "unit", containerId = 1)
  }

  val workerGainGate = new CountDownLatch(1)

  /**
   * startup services used locally to any individual unit test scenario
   */
  protected
  def localStartup(): Unit

  protected
  def localAfterStartup(): Unit = {}

  /**
   * shutdown services used locally to any individual unit test scenario
   */
  protected
  def localShutdown(): Unit

  final override protected
  def beforeAll(): Unit = {
    try {

      supervisorContainer.metadata withLookup this
      supervisorContainer.topology talksTo this

      localStartup()

      supervisorContainer.start
      workerContainer.start

      // wait for the local worker to be available before trying anything
      workerGainGate.await()

      localAfterStartup()
    } catch safely {
      case t: Throwable =>
        log error burstStdMsg(t)
        throw t
    }
  }

  final override protected
  def afterAll(): Unit = {
    localShutdown()
    cache.instance.stop
    supervisorContainer.stop
    workerContainer.stop
    TeslaFactoryBoss.assertNoInUseParts()
  }

  final override def onTopologyWorkerGained(worker: FabricTopologyWorker): Unit = {
    workerGainGate.countDown()
  }
}
