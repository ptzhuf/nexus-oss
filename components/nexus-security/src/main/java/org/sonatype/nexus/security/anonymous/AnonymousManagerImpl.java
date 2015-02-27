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
package org.sonatype.nexus.security.anonymous;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.Mutex;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: Move to internal

/**
 * Default {@link AnonymousManagerImpl}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class AnonymousManagerImpl
  extends ComponentSupport
  implements AnonymousManager
{
  private final AnonymousConfigurationStore store;

  private final Provider<AnonymousConfiguration> defaults;

  private final Mutex lock = new Mutex();

  private AnonymousConfiguration configuration;

  @Inject
  public AnonymousManagerImpl(final AnonymousConfigurationStore store,
                              final @Named("initial") Provider<AnonymousConfiguration> defaults)
  {
    this.store = checkNotNull(store);
    this.defaults = checkNotNull(defaults);
  }

  //
  // Configuration
  //

  /**
   * Load configuration from store, or use defaults.
   */
  private AnonymousConfiguration loadConfiguration() {
    AnonymousConfiguration model = store.load();

    // use defaults if no configuration was loaded from the store
    if (model == null) {
      model = defaults.get();

      // default config must not be null
      checkNotNull(model);

      log.info("Using default configuration: {}", model);
    }
    else {
      log.info("Loaded configuration: {}", model);
    }

    return model;
  }

  /**
   * Return configuration, loading if needed.
   */
  private AnonymousConfiguration getConfigurationInternal() {
    synchronized (lock) {
      if (configuration == null) {
        configuration = loadConfiguration();
      }
      return configuration;
    }
  }

  /**
   * Return _copy_ of configuration.
   */
  @Override
  public AnonymousConfiguration getConfiguration() {
    return getConfigurationInternal().copy();
  }

  @Override
  public void setConfiguration(final AnonymousConfiguration configuration) {
    checkNotNull(configuration);

    AnonymousConfiguration model = configuration.copy();
    // TODO: Validate configuration before saving?  Or leave to ext.direct?  Should we try and verify the user exists?

    log.info("Saving configuration: {}", model);
    synchronized (lock) {
      store.save(model);
      this.configuration = model;
    }
  }

  //
  // Helpers
  //

  @Override
  public boolean isEnabled() {
    return getConfigurationInternal().isEnabled();
  }

  @Override
  public Subject buildSubject() {
    AnonymousConfiguration model = getConfigurationInternal();

    log.info("Building anonymous subject with user-id: {}, realm-name: {}", model.getUserId(), model.getRealmName());

    // custom principals to aid with anonymous subject detection
    PrincipalCollection principals = new AnonymousPrincipalCollection(
        model.getUserId(),
        model.getRealmName()
    );

    return new Subject.Builder()
        .principals(principals)
        .authenticated(false)
        .buildSubject();
  }

  @Override
  public boolean isAnonymous(final Subject subject) {
    checkNotNull(subject);
    return subject.getPrincipals() instanceof AnonymousPrincipalCollection;
  }
}
