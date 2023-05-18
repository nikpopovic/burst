/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.alloy.usecase

import org.burstsys.alloy.store.mini.MiniView
import org.burstsys.fabric.wave.data.model.generation.key.FabricGenerationKey
import org.burstsys.fabric.wave.metadata.model.domain.FabricDomain
import org.burstsys.fabric.wave.metadata.model.view.FabricView
import org.burstsys.fabric.wave.metadata.model.{FabricDomainKey, FabricMetadataLookup, FabricViewKey}
import org.burstsys.vitals.errors.VitalsException
import org.burstsys.vitals.properties.VitalsPropertyMap

import scala.util.{Failure, Success, Try}

trait UnitMetadataLookup extends FabricMetadataLookup {

  protected def localViews: Array[MiniView]

  final override
  def domainLookup(key: FabricDomainKey): Try[FabricDomain] = {
    localViews.find(_.domainKey == key) match {
      case None => Failure(VitalsException().fillInStackTrace())
      case Some(v) => Success(v.domain)
    }
  }

  final override
  def viewLookup(key: FabricViewKey, validate: Boolean = false): Try[FabricView] = {
    localViews.find(_.viewKey == key) match {
      case None => Failure(VitalsException().fillInStackTrace())
      case Some(v) => Success(v.view)
    }
  }

  final override
  def recordViewLoad(key: FabricGenerationKey, updatedProperties: VitalsPropertyMap): Try[Boolean] = {
    localViews.find(_.viewKey == key.viewKey) match {
      case None => Failure(VitalsException().fillInStackTrace())
      case Some(v) => Success(false)
    }
  }
}
