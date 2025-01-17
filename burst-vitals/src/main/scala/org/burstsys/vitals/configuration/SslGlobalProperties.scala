/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.vitals.configuration

trait SslGlobalProperties extends Any {

  /**
    * the path to the X509 certificate to present to clients
    */
  def certPath: String = burstSslCertPath.getOrThrow

  /**
    * the path to the private key for `certPath`
    */
  def keyPath: String = burstSslKeyPath.getOrThrow

  /**
    * if you want a global location for SSL CA
    * @return
    */
  def caPath: String = burstTrustedCaPath.getOrThrow

  /**
    * disables validation on SSL connections (should only be used in local development)
    */
  def enableCompositeTrust: Boolean = burstEnableCompositeTrust.getOrThrow
}
