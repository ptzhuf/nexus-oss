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

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.common.collect.Sets;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.apache.shiro.web.subject.WebSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anonymous filter, that should be LAST in the filter chain for paths allowing anonymous users.
 * Note: "anonymous" user is NOT "Shiro guest"! It does not perform logging in. It fiddles with swapping out Subject
 * instance.
 *
 * @since 3.0
 */
@Named
@Singleton
public class AnonymousFilter
    extends AdviceFilter
{
  private static final Logger log = LoggerFactory.getLogger(AnonymousFilter.class);

  public static final String NAME = "anonymous";

  private final AnonymousConfiguration anonymousConfiguration;

  private final RolePermissionResolver rolePermissionResolver;

  @Inject
  public AnonymousFilter(final AnonymousConfiguration anonymousConfiguration,
                         final RolePermissionResolver rolePermissionResolver)
  {
    this.anonymousConfiguration = anonymousConfiguration;
    this.rolePermissionResolver = rolePermissionResolver;
  }

  private Set<Permission> resolvePermissions(final Set<String> roles) {
    final Set<Permission> permissions = Sets.newHashSet();
    for (String role : roles) {
      permissions.addAll(rolePermissionResolver.resolvePermissionsInRole(role));
    }
    return permissions;
  }

  private AnonymousSubject getAndWrap() {
    final Subject subject = SecurityUtils.getSubject();
    if (subject instanceof AnonymousSubject) {
      return (AnonymousSubject) subject;
    }
    // don't spend cpu cycles if disabled, but still, the subject should be replaced
    // TODO: if enabled, these things could be cached, and rebuilt on config change
    final boolean enabled = anonymousConfiguration.isEnabled();
    final PrincipalCollection anonymousIdentity = enabled ?
        new SimplePrincipalCollection(anonymousConfiguration.getPrincipal(), "n/a") : null;
    final Set<String> roles = enabled ?
        anonymousConfiguration.getRoles() : Collections.<String>emptySet();
    final Set<Permission> permissions = enabled ?
        resolvePermissions(roles) : Collections.<Permission>emptySet();
    final AnonymousSubject wrapped;
    if (subject instanceof WebSubject) {
      wrapped = new AnonymousWebSubject(anonymousIdentity, roles, permissions, (WebSubject) subject);
    }
    else {
      wrapped = new AnonymousSubject(anonymousIdentity, roles, permissions, subject);
    }
    return wrapped;
  }

  @Override
  protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
    final AnonymousSubject subject = getAndWrap();
    ThreadContext.bind(subject);
    if (subject.getPrincipal() == null) {
      log.info("Setting anonymous");
      subject.setAnonymous();
    }
    return true;
  }

  @Override
  public void afterCompletion(ServletRequest request, ServletResponse response, Exception exception) throws Exception {
    final AnonymousSubject subject = getAndWrap();
    if (subject.isAnonymous()) {
      log.info("Unsetting anonymous");
      subject.unsetAnonymous();
    }
  }
}
