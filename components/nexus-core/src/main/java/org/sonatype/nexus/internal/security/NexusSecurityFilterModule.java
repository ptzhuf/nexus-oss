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
package org.sonatype.nexus.internal.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.FilterProviderSupport;
import org.sonatype.nexus.security.authz.AnonymousFilter;
import org.sonatype.nexus.web.FailureLoggingHttpMethodPermissionFilter;

import com.google.inject.AbstractModule;

import static org.sonatype.nexus.security.FilterProviderSupport.filterKey;

/**
 * Sets up Nexus's security filter configuration; this is a @Named module so it will be auto-installed by Sisu.
 */
@Named
public class NexusSecurityFilterModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(filterKey(AnonymousFilter.NAME)).to(AnonymousFilter.class).in(Singleton.class);

    bind(filterKey("authcBasic")).toProvider(AuthcBasicFilterProvider.class);
    bind(filterKey("perms")).to(FailureLoggingHttpMethodPermissionFilter.class).in(Singleton.class);
    bind(filterKey("authcApiKey")).toProvider(AuthcApiKeyFilterProvider.class);
  }

  @Singleton
  static class AuthcBasicFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    AuthcBasicFilterProvider(final NexusAuthenticationFilter filter) {
      super(filter);
      filter.setApplicationName("Sonatype Nexus Repository Manager API");
      filter.setFakeAuthScheme(Boolean.toString(false));
    }
  }

  @Singleton
  static class AuthcApiKeyFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    AuthcApiKeyFilterProvider(final NexusApiKeyAuthenticationFilter filter) {
      super(filter);
      filter.setApplicationName("Sonatype Nexus Repository Manager API (X-...-ApiKey auth)");
    }
  }
}
