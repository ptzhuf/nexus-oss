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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

public class AnonymousSubject
    extends ComponentSupport
    implements Subject
{
  private final AnonymousConfiguration anonymousConfiguration;

  private final PermissionResolver permissionResolver;

  private final Set<String> roles;

  private final Set<Permission> permissions;

  private final Subject subject;

  private PrincipalCollection anonymous;

  public AnonymousSubject(final AnonymousConfiguration anonymousConfiguration,
                          final Set<String> roles,
                          final Set<Permission> permissions,
                          final Subject subject)
  {
    this.anonymousConfiguration = checkNotNull(anonymousConfiguration);
    this.permissionResolver = new WildcardPermissionResolver(); // TODO: this to be injected?
    this.roles = checkNotNull(roles);
    this.permissions = checkNotNull(permissions);
    this.subject = checkNotNull(subject);
  }

  private boolean permitted(final Permission permission) {
    if (!permissions.isEmpty()) {
      for (Permission perm : permissions) {
        if (perm.implies(permission)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean permitted(final String permission) {
    return permitted(permissionResolver.resolvePermission(permission));
  }

  private void check(final Permission permission) throws AuthorizationException {
    if (!permitted(permission)) {
      throw new UnauthorizedException();
    }
  }

  private void check(final String permission) throws AuthorizationException {
    check(permissionResolver.resolvePermission(permission));
  }

  public void setAnonymous() {
    if (anonymousConfiguration.isEnabled()) {
      this.anonymous = new SimplePrincipalCollection(anonymousConfiguration.getPrincipal(), "n/a");
    }
  }

  public void unsetAnonymous() {
    this.anonymous = null;
  }

  public boolean isAnonymous() {
    return anonymous != null;
  }

  public Subject getSubject() {
    return subject;
  }

  public PrincipalCollection getAnonymousPrincipals() {
    if (isAnonymous()) {
      return anonymous;
    }
    else {
      return null;
    }
  }

  @Override
  public Object getPrincipal() {
    return subject.getPrincipal();
  }

  @Override
  public PrincipalCollection getPrincipals() {
    return subject.getPrincipals();
  }

  @Override
  public boolean isPermitted(final String permission) {
    if (isAnonymous()) {
      return permitted(permissionResolver.resolvePermission(permission));
    }
    return subject.isPermitted(permission);
  }

  @Override
  public boolean isPermitted(final Permission permission) {
    if (isAnonymous()) {
      return permitted(permission);
    }
    return subject.isPermitted(permission);
  }

  @Override
  public boolean[] isPermitted(final String... permissions) {
    if (isAnonymous()) {
      boolean[] result = new boolean[permissions.length];
      for (int i = 0; i < permissions.length; i++) {
        result[i] = permitted(permissions[i]);
      }
      return result;
    }
    return subject.isPermitted(permissions);
  }

  @Override
  public boolean[] isPermitted(final List<Permission> permissions) {
    if (isAnonymous()) {
      boolean[] result = new boolean[permissions.size()];
      for (int i = 0; i < permissions.size(); i++) {
        result[i] = permitted(permissions.get(i));
      }
      return result;
    }
    return subject.isPermitted(permissions);
  }

  @Override
  public boolean isPermittedAll(final String... permissions) {
    if (isAnonymous()) {
      for (String permission : permissions) {
        if (!permitted(permission)) {
          return false;
        }
      }
      return true;
    }
    return subject.isPermittedAll(permissions);
  }

  @Override
  public boolean isPermittedAll(final Collection<Permission> permissions) {
    if (isAnonymous()) {
      for (Permission permission : permissions) {
        if (!permitted(permission)) {
          return false;
        }
      }
      return true;
    }
    return subject.isPermittedAll(permissions);
  }

  @Override
  public void checkPermission(final String permission) throws AuthorizationException {
    if (isAnonymous()) {
      check(permission);
    }
    else {
      subject.checkPermission(permission);
    }
  }

  @Override
  public void checkPermission(final Permission permission) throws AuthorizationException {
    if (isAnonymous()) {
      check(permission);
    }
    else {
      subject.checkPermission(permission);
    }
  }

  @Override
  public void checkPermissions(final String... permissions) throws AuthorizationException {
    if (isAnonymous()) {
      for (String permission : permissions) {
        check(permission);
      }
    }
    else {
      subject.checkPermissions(permissions);
    }
  }

  @Override
  public void checkPermissions(final Collection<Permission> permissions) throws AuthorizationException {
    if (isAnonymous()) {
      for (Permission permission : permissions) {
        check(permission);
      }
    }
    else {
      subject.checkPermissions(permissions);
    }
  }

  @Override
  public boolean hasRole(final String roleIdentifier) {
    if (isAnonymous()) {
      return roles.contains(roleIdentifier);
    }
    else {
      return subject.hasRole(roleIdentifier);
    }
  }

  @Override
  public boolean[] hasRoles(final List<String> roleIdentifiers) {
    if (isAnonymous()) {
      boolean[] result = new boolean[roleIdentifiers.size()];
      for (int i = 0; i < roleIdentifiers.size(); i++) {
        result[i] = hasRole(roleIdentifiers.get(i));
      }
      return result;
    }
    else {
      return subject.hasRoles(roleIdentifiers);
    }
  }

  @Override
  public boolean hasAllRoles(final Collection<String> roleIdentifiers) {
    if (isAnonymous()) {
      for (String roleIdentifier : roleIdentifiers) {
        if (!hasRole(roleIdentifier)) {
          return false;
        }
      }
      return true;
    }
    else {
      return subject.hasAllRoles(roleIdentifiers);
    }
  }

  @Override
  public void checkRole(final String roleIdentifier) throws AuthorizationException {
    if (isAnonymous()) {
      if (!hasRole(roleIdentifier)) {
        throw new UnauthorizedException("User does not have required role");
      }
    }
    else {
      subject.checkRole(roleIdentifier);
    }
  }

  @Override
  public void checkRoles(final Collection<String> roleIdentifiers) throws AuthorizationException {
    if (isAnonymous()) {
      for (String roleIdentifier : roleIdentifiers) {
        checkRole(roleIdentifier);
      }
    }
    else {
      subject.checkRoles(roleIdentifiers);
    }
  }

  @Override
  public void checkRoles(final String... roleIdentifiers) throws AuthorizationException {
    if (isAnonymous()) {
      for (String roleIdentifier : roleIdentifiers) {
        checkRole(roleIdentifier);
      }
    }
    else {
      subject.checkRoles(roleIdentifiers);
    }
  }

  @Override
  public boolean isAuthenticated() {
    return !isAnonymous() && subject.isAuthenticated();
  }

  @Override
  public boolean isRemembered() {
    return !isAnonymous() && subject.isRemembered();
  }

  @Override
  public Session getSession() {
    return subject.getSession();
  }

  @Override
  public Session getSession(final boolean create) {
    return subject.getSession(!isAnonymous() && create);
  }

  @Override
  public void login(final AuthenticationToken token) throws AuthenticationException {
    subject.login(token);
    unsetAnonymous(); // if above throws, this will not get unset
  }

  @Override
  public void logout() {
    subject.logout();
    unsetAnonymous();
  }

  @Override
  public <V> V execute(final Callable<V> callable) throws ExecutionException {
    return subject.execute(callable);
  }

  @Override
  public void execute(final Runnable runnable) {
    subject.execute(runnable);
  }

  @Override
  public <V> Callable<V> associateWith(final Callable<V> callable) {
    return subject.associateWith(callable);
  }

  @Override
  public Runnable associateWith(final Runnable runnable) {
    return subject.associateWith(runnable);
  }

  @Override
  public void runAs(final PrincipalCollection principals) throws NullPointerException, IllegalStateException {
    subject.runAs(principals);
  }

  @Override
  public boolean isRunAs() {
    return subject.isRunAs();
  }

  @Override
  public PrincipalCollection getPreviousPrincipals() {
    return subject.getPreviousPrincipals();
  }

  @Override
  public PrincipalCollection releaseRunAs() {
    return subject.releaseRunAs();
  }
}
