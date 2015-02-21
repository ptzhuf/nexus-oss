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
package org.sonatype.nexus.security.internal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.throwables.ConfigurationException;
import org.sonatype.nexus.common.validation.ValidationResponse;
import org.sonatype.nexus.common.validation.ValidationResponseException;
import org.sonatype.nexus.security.settings.SecuritySettings;
import org.sonatype.nexus.security.settings.SecuritySettingsManager;
import org.sonatype.nexus.security.settings.SecuritySettingsSource;
import org.sonatype.nexus.security.settings.SecuritySettingsValidationContext;
import org.sonatype.nexus.security.settings.SecuritySettingsValidator;
import org.sonatype.sisu.goodies.common.ComponentSupport;

@Named
@Singleton
public class SecuritySettingsManagerImpl
    extends ComponentSupport
    implements SecuritySettingsManager
{
  private final SecuritySettingsSource configurationSource;

  private final SecuritySettingsValidator validator;

  /**
   * This will hold the current configuration in memory, to reload, will need to set this to null
   */
  private SecuritySettings configuration = null;

  private ReentrantLock lock = new ReentrantLock();

  @Inject
  public SecuritySettingsManagerImpl(final SecuritySettingsSource configurationSource,
                                     final SecuritySettingsValidator validator)
  {
    this.configurationSource = configurationSource;
    this.validator = validator;
  }

  @Override
  public boolean isAnonymousAccessEnabled() {
    return getConfiguration().isAnonymousAccessEnabled();
  }

  @Override
  public void setAnonymousAccessEnabled(boolean anonymousAccessEnabled) {
    getConfiguration().setAnonymousAccessEnabled(anonymousAccessEnabled);
  }

  @Override
  public String getAnonymousPassword() {
    return getConfiguration().getAnonymousPassword();
  }

  @Override
  public void setAnonymousPassword(String anonymousPassword) throws ConfigurationException {
    ValidationResponse vr = validator.validateAnonymousPassword(initializeContext(), anonymousPassword);

    if (vr.isValid()) {
      getConfiguration().setAnonymousPassword(anonymousPassword);
    }
    else {
      throw new ValidationResponseException(vr);
    }
  }

  @Override
  public String getAnonymousUsername() {
    return getConfiguration().getAnonymousUsername();
  }

  @Override
  public void setAnonymousUsername(String anonymousUsername) throws ConfigurationException {
    ValidationResponse vr = validator.validateAnonymousUsername(initializeContext(), anonymousUsername);

    if (vr.isValid()) {
      getConfiguration().setAnonymousUsername(anonymousUsername);
    }
    else {
      throw new ValidationResponseException(vr);
    }
  }

  @Override
  public List<String> getRealms() {
    return Collections.unmodifiableList(getConfiguration().getRealms());
  }

  @Override
  public void setRealms(List<String> realms) throws ConfigurationException {
    ValidationResponse vr = validator.validateRealms(initializeContext(), realms);

    if (vr.isValid()) {
      getConfiguration().setRealms(realms);
    }
    else {
      throw new ValidationResponseException(vr);
    }
  }

  private SecuritySettings getConfiguration() {
    if (configuration != null) {
      return configuration;
    }

    lock.lock();

    try {
      configurationSource.loadConfiguration();

      configuration = configurationSource.getConfiguration();
    }
    finally {
      lock.unlock();
    }

    return configuration;
  }

  @Override
  public void clearCache() {
    // Just to make sure we aren't fiddling w/ save/loading process
    lock.lock();
    try {
      configuration = null;
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void save() {
    lock.lock();

    try {
      configurationSource.storeConfiguration();
    }
    finally {
      lock.unlock();
    }
  }

  private SecuritySettingsValidationContext initializeContext() {
    SecuritySettingsValidationContext context = new SecuritySettingsValidationContext();
    context.setSecuritySettings(getConfiguration());
    return context;
  }
}
