/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus;

/**
 * Nexus system state object. It gives small amount of important infos about Nexus Application.
 *
 * @author cstamas
 * @author damian
 */
public class SystemStatus
{
  /**
   * The Nexus Application version.
   */
  private String version = "unknown";

  /**
   * The Nexus Application edition for user agent
   */
  private String editionShort = "OSS";

  /**
   * The Nexus Application state.
   */
  private SystemState state;

  /**
   * True if a license is installed, false otherwise. For OSS always return false.
   */
  private boolean licenseInstalled = false;

  /**
   * True if license is expired, false otherwise. For OSS always return false.
   */
  private boolean licenseExpired = false;

  /**
   * True if installed license is a trial license, false otherwise. For OSS always return false.
   */
  private boolean trialLicense = false;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getEditionShort() {
    return editionShort;
  }

  public void setEditionShort(String editionUserAgent) {
    this.editionShort = editionUserAgent;
  }

  public SystemState getState() {
    return state;
  }

  public void setState(SystemState status) {
    this.state = status;
  }

  public boolean isNexusStarted() {
    return SystemState.STARTED.equals(getState());
  }

  public boolean isLicenseInstalled() {
    return licenseInstalled;
  }

  public void setLicenseInstalled(final boolean licenseInstalled) {
    this.licenseInstalled = licenseInstalled;
  }

  public boolean isLicenseExpired() {
    return licenseExpired;
  }

  public void setLicenseExpired(final boolean licenseExpired) {
    this.licenseExpired = licenseExpired;
  }

  public boolean isTrialLicense() {
    return trialLicense;
  }

  public void setTrialLicense(final boolean trialLicense) {
    this.trialLicense = trialLicense;
  }

}
