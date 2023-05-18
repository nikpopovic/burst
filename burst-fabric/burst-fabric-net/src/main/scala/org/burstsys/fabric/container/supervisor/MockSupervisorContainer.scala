/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.fabric.container.supervisor

import org.burstsys.fabric.configuration
import org.burstsys.fabric.container.FabricContainerId
import org.burstsys.fabric.net.FabricNetworkConfig
import org.burstsys.fabric.net.server.unitFabricNetworkServerConfig
import org.burstsys.tesla.part.factory.TeslaFactoryBoss
import org.burstsys.vitals.logging._
import org.burstsys.vitals.sysinfo.SystemInfo
import org.burstsys.{tesla, vitals}

/**
 * a container for unit tests
 */
trait MockSupervisorContainer[T <: FabricSupervisorListener] extends FabricSupervisorContainer[T]

object MockSupervisorContainer {
  /**
   * constructor for situations where you know your container id in advance (such as unit tests)
   *
   * @param logFile
   * @param containerId
   * @return
   */
  def apply[T <: FabricSupervisorListener](logFile: String, containerId: FabricContainerId,
            netConfig: FabricNetworkConfig = unitFabricNetworkServerConfig): MockSupervisorContainer[T] = {
    configuration.burstFabricSupervisorStandaloneProperty.set(true)
    vitals.configuration.configureForUnitTests()
    tesla.configuration.configureForUnitTests()
    val c = MockSupervisorContainerContext[T](logFile: String, netConfig)
    c.containerId = containerId
    c
  }

}

private final case
class MockSupervisorContainerContext[T <: FabricSupervisorListener](logFile: String, netConfig: FabricNetworkConfig)
  extends FabricSupervisorContainerContext[T](netConfig) with MockSupervisorContainer[T] {

  override def serviceName: String = s"mock-supervisor-container"

  override def log4JPropertiesFileName: String = logFile

  //////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Lifecycle
  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * This is the heart of the container lifecycle
   */
  override
  def start: this.type = {
    synchronized {
      ensureNotRunning

      System.setProperty("tesla.parts.tender.frequency", 30.toString)

      // initialize as a test
      VitalsLog.configureLogging(log4JPropertiesFileName, consoleOnly = true)

      log info startingMessage
      // start the underlying fabric container
      super.start
      log info startedWithDateMessage

      markRunning
    }
    this
  }


  /**
   * This is the heart of the container lifecycle
   */
  override
  def stop: this.type = {
    synchronized {
      ensureRunning
      log info stoppingMessage
      super.stop

      markNotRunning
      log info stoppedWithDateMessage

      TeslaFactoryBoss.assertNoInUseParts()
    }
    this
  }
}
