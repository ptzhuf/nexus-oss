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

package org.sonatype.nexus.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.shiro.web.filter.mgt.FilterChainManager;

import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Remove use the FilterChainManager directly?

/**
 * Default {@link ProtectedPathManager}.
 */
@Named
@Singleton
public class ProtectedPathManagerImpl
    extends ComponentSupport
    implements ProtectedPathManager
{
  private final FilterChainManager filterChainManager;

  @Inject
  public ProtectedPathManagerImpl(final FilterChainManager filterChainManager) {
    this.filterChainManager = checkNotNull(filterChainManager);
  }

  public void addProtectedResource(final String pathPattern, final String filterExpression) {
    log.debug("Adding protected resource: path={}, filter={}", pathPattern, filterExpression);
    filterChainManager.createChain(pathPattern, filterExpression);
  }
}