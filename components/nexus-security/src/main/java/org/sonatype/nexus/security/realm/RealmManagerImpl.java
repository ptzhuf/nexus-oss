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
package org.sonatype.nexus.security.realm;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.Mutex;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RealmManager}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RealmManagerImpl
  extends ComponentSupport
  implements RealmManager
{
  private final RealmConfigurationStore store;

  private final Provider<RealmConfiguration> defaults;

  private final Mutex lock = new Mutex();

  private RealmConfiguration configuration;

  @Inject
  public RealmManagerImpl(final RealmConfigurationStore store,
                          final @Named("initial") Provider<RealmConfiguration> defaults)
  {
    this.store = checkNotNull(store);
    this.defaults = checkNotNull(defaults);
  }
}
