/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.zap.route.factory

import org.burstsys.tesla.part.TeslaPartShop
import org.burstsys.zap.route
import org.burstsys.zap.route.{ZapRoute, ZapRouteContext, ZapRouteBuilder}

trait ZapRouteShop extends TeslaPartShop[ZapRoute, ZapRouteBuilder] {

  def partName: String = route.partName

  /**
   *
   * @param schema
   * @return
   */
  def grabZapRoute(schema: ZapRouteBuilder): ZapRoute

  /**
   *
   * @param route
   */
  def releaseZapRoute(route: ZapRoute): Unit

}
