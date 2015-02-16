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

import java.util.Collections;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

/**
 * Immutable configuration data, retrieved from some configuration source and used during one single HTTP call.
 */
@Immutable
public class Configuration
{
  /**
   * Is anonymous user support enabled?
   */
  private final boolean enabled;

  /**
   * Can anonymous user create sessions? Overrides global shiro config, having false here will prevent session creation
   * for anonymous users even if session creation is enabled at shiro level. It applies to anonymous (Shiro "guest")
   * users only.
   */
  private final boolean sessionCreationEnabled;

  /**
   * Principal to use for anonymous user, for example "anonymous" or any other principal will work.
   */
  private final Object principal;

  /**
   * If not {@code null}, will "map" {@code principal} to given realm to perform authorization.
   */
  private final String originatingRealm;

  /**
   * If not mapped to a realm, this is the set of roles anonymous will have, and based on these will permissions be
   * resolved too.
   */
  private final Set<String> roles;

  private final Set<Permission> permissions;

  private final PrincipalCollection principalCollection;

  public Configuration(final boolean enabled,
                       final boolean sessionCreationEnabled,
                       final Object principal,
                       final String originatingRealm,
                       final Set<String> roles,
                       final Set<Permission> permissions)
  {
    this.enabled = enabled;
    if (this.enabled) {
      this.sessionCreationEnabled = sessionCreationEnabled;
      this.principal = principal;
      this.originatingRealm = originatingRealm;
      this.roles = roles == null ? Collections.<String>emptySet() : ImmutableSet.copyOf(roles);
      this.permissions = permissions == null ? Collections.<Permission>emptySet() : ImmutableSet.copyOf(permissions);

      this.principalCollection = new SimplePrincipalCollection(principal,
          originatingRealm == null ? "n/a" : originatingRealm);
    }
    else {
      this.sessionCreationEnabled = true;
      this.principal = null;
      this.originatingRealm = null;
      this.roles = Collections.emptySet();
      this.permissions = Collections.emptySet();

      this.principalCollection = null;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isSessionCreationEnabled() {
    return sessionCreationEnabled;
  }

  public Object getPrincipal() {
    return principal;
  }

  public String getOriginatingRealm() {
    return originatingRealm;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public Set<Permission> getPermissions() {
    return permissions;
  }

  public PrincipalCollection getPrincipalCollection() {
    return principalCollection;
  }
}