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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.concurrent.Locks;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
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

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private AnonymousConfiguration configuration;

  @Inject
  public AnonymousManagerImpl(final AnonymousConfigurationStore store) {
    this.store = checkNotNull(store);
  }

  @Override
  public AnonymousConfiguration getConfiguration() {
    Lock lock = Locks.read(readWriteLock);
    try {
      if (configuration == null) {
        configuration = store.load();
        log.info("Loaded configuration: {}", configuration);
      }
      return configuration;
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void setConfiguration(final AnonymousConfiguration configuration) {
    checkNotNull(configuration);

    // TODO: Validate configuration before saving

    Lock lock = Locks.write(readWriteLock);
    try {
      log.info("Saving configuration: {}", configuration);
      store.save(configuration);
      this.configuration = configuration.copy();

      // TODO: Sort out authc events to flush credentials
      // TODO: Sort out other User bits which DefaultSecuritySystem.*anonymous* bits are doing
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isEnabled() {
    return getConfiguration().isEnabled();
  }

  @Override
  public Subject buildSubject() {
    AnonymousConfiguration config = getConfiguration();

    PrincipalCollection principals = new SimplePrincipalCollection(
        config.getUserId(),
        config.getRealmName());

    log.info("Building anonymous subject with principals: {}", principals);

    return new Subject.Builder()
        .principals(principals)
        .authenticated(false)
        .buildSubject();
  }

  // TODO: Add isAnonymous(Subject) helper?  Could potentially use a custom PrincipalCollection to help?
}
