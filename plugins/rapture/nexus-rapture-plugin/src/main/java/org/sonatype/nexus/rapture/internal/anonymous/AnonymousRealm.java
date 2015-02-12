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
package org.sonatype.nexus.rapture.internal.anonymous;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.Sets;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Realm managing anonymous user. It hooks into rest of Realms present, by providing only a role(s) of anymous user,
 * while actual permissions are being resolved as every other existing role. The actual role(s) are coming from some
 * configuration, while the permissions assigned to role(s) are coming from usual source of roles for the rest of
 * the system (NexusRealm, LDAP, or anything).
 *
 * @since 3.0
 */
@Singleton
@Named(AnonymousRealm.NAME)
@Description(AnonymousRealm.DESCRIPTION)
public class AnonymousRealm
    extends AuthorizingRealm
{
  public static final String NAME = "Anonymous";

  public static final String DESCRIPTION = "Anonymous Access Realm";

  public static final Object PRINCIPAL = "anonymous";

  public static final Object CREDENTIALS = null;

  public static final PrincipalCollection PRINCIPAL_COLLECTION =
      new SimplePrincipalCollection(PRINCIPAL, NAME);

  public static final AuthenticationInfo AUTHENTICATION_INFO =
      new SimpleAuthenticationInfo(PRINCIPAL_COLLECTION, CREDENTIALS);

  @Inject
  public AnonymousRealm(final RolePermissionResolver rolePermissionResolver) {
    checkNotNull(rolePermissionResolver);
    setName(NAME); // for cache naming
    setAuthorizationCachingEnabled(true); // do cache authz result
    setRolePermissionResolver(rolePermissionResolver); // source to resolve configured role(s) for anonymous
  }

  @Override
  public boolean supports(final AuthenticationToken token) {
    return false;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) throws AuthenticationException {
    // just return the same immutable
    return AUTHENTICATION_INFO;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
    if (principals.getRealmNames().contains(getName())) {
      // lookup from config, as a) result is cached, b) configuration (anon roles) might change
      // TODO: "anonymous" role name should/could come from configuration
      return new SimpleAuthorizationInfo(
          Sets.newHashSet("anonymous")); // that's all folks, and leave Shiro to resolve it
    }
    else {
      return null;
    }
  }
}
