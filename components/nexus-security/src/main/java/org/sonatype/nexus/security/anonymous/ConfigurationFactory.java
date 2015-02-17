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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.internal.AuthenticatingRealmImpl;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Current example in-memory configuration. This would refer to some real store or configuration source.
 */
@Singleton
@Named
public class ConfigurationFactory
{
  private final RolePermissionResolver rolePermissionResolver;

  private boolean enabled;

  private boolean sessionCreationEnabled;

  private String principal;

  private String originatingRealm;

  private Set<String> roles;

  @Inject
  public ConfigurationFactory() { //final RolePermissionResolver rolePermissionResolver) {
    // TODO: role permResolver is NOT needed now (as anon user is mapped), but would be needed to generic operation
    // TODO: Still, due some guicey/Sisuey wiring problem I cannot get a role perm resolver here
    //this.rolePermissionResolver = checkNotNull(rolePermissionResolver);
    this.rolePermissionResolver = null;
    // TODO: these are just example configuratiuon, emulating what NX2 had, except no "anonymous" user needed in XML realm
    this.enabled = true;
    this.sessionCreationEnabled = false;
    this.principal = "anonymous"; // map it to anonymous user
    this.originatingRealm = AuthenticatingRealmImpl.NAME; // map it to "XML" realm
    this.roles = Collections.emptySet();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isSessionCreationEnabled() {
    return sessionCreationEnabled;
  }

  public void setSessionCreationEnabled(final boolean sessionCreationEnabled) {
    this.sessionCreationEnabled = sessionCreationEnabled;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(final String principal) {
    this.principal = principal;
  }

  public String getOriginatingRealm() {
    return originatingRealm;
  }

  public void setOriginatingRealm(final String originatingRealm) {
    this.originatingRealm = originatingRealm;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setRoles(final Set<String> roles) {
    this.roles = roles;
  }

  public Configuration getConfiguration() {
    // TODO: caching, would need to rebuild this only on config change, otherwise reuse a pre-created immutable instance
    final Set<Permission> permissions = Sets.newHashSet();
    for (String role : roles) {
      permissions.addAll(rolePermissionResolver.resolvePermissionsInRole(role));
    }
    return new Configuration(enabled, sessionCreationEnabled, principal, originatingRealm, roles, permissions);
  }
}