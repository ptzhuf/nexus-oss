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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DisabledSessionException;
import org.apache.shiro.subject.support.SubjectCallable;
import org.apache.shiro.subject.support.SubjectRunnable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Shiro subject implementation that is able to grant permissions to anonymous users (Shiro "guests"). To use this
 * implementation, you need two things:
 * <ul>
 * <li>Register and use {@link SubjectFactory} as global subject factory</li>
 * <li>Add a filter to entry points (like servlet filter for webapps) where you call {@link #setAnonymous()} method on
 * entry, and {@link #unsetAnonymous()} on exit. This is the one and only place where casting of Subject is needed,
 * everything else works as usual.</li>
 * </ul>
 * This implementation does not change behaviour of any existing Shiro filter, but it makes possible to assign roles
 * and permissions to non-authenticated subjects.
 */
public class AnonymousSubject
    implements Subject
{
  private final ConfigurationFactory anonymousConfigurationSource;

  private final SecurityManager securityManager;

  private final PermissionResolver permissionResolver;

  private final Subject subject;

  private Configuration anonymousConfiguration;

  public AnonymousSubject(final ConfigurationFactory anonymousConfigurationSource,
                          final SecurityManager securityManager,
                          final PermissionResolver permissionResolver,
                          final Subject subject)
  {
    this.anonymousConfigurationSource = checkNotNull(anonymousConfigurationSource);
    this.securityManager = checkNotNull(securityManager);
    this.permissionResolver = checkNotNull(permissionResolver);
    this.subject = checkNotNull(subject);
  }

  private boolean permitted(final Permission permission) {
    if (anonymousConfiguration.getOriginatingRealm() != null) {
      return securityManager.isPermitted(getAnonymousPrincipals(), permission);
    }
    else {
      if (!anonymousConfiguration.getPermissions().isEmpty()) {
        for (Permission perm : anonymousConfiguration.getPermissions()) {
          if (perm.implies(permission)) {
            return true;
          }
        }
      }
      return false;
    }
  }

  private boolean permitted(final String permission) {
    return permitted(permissionResolver.resolvePermission(permission));
  }

  private void check(final Permission permission) throws AuthorizationException {
    if (!permitted(permission)) {
      throw new UnauthorizedException("Anonymous user does not have " + permission + " permission");
    }
  }

  private void check(final String permission) throws AuthorizationException {
    check(permissionResolver.resolvePermission(permission));
  }

  private void reset() {
    this.anonymousConfiguration = null;
  }

  public void setAnonymous() {
    // fetch configuration as subject instances are reused, so get current config always
    final Configuration configuration = anonymousConfigurationSource.getConfiguration();
    // remembered and authenticated users have principals, guard against both
    if (configuration.isEnabled() && subject.getPrincipals() == null) {
      subject.logout();
      this.anonymousConfiguration = configuration;
    }
  }

  public void unsetAnonymous() {
    if (anonymousConfiguration != null) {
      subject.logout();
      reset();
    }
  }

  public boolean isAnonymous() {
    return anonymousConfiguration != null;
  }

  public Subject getSubject() {
    return subject;
  }

  public PrincipalCollection getAnonymousPrincipals() {
    if (isAnonymous()) {
      return anonymousConfiguration.getPrincipalCollection();
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
      if (anonymousConfiguration.getOriginatingRealm() != null) {
        return securityManager.hasRole(getAnonymousPrincipals(), roleIdentifier);
      }
      else {
        return anonymousConfiguration.getRoles().contains(roleIdentifier);
      }
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
        throw new UnauthorizedException("Anonymous user does not have " + roleIdentifier + " role");
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
    return getSession(true);
  }

  @Override
  public Session getSession(final boolean create) {
    if (create && anonymousConfiguration != null && !anonymousConfiguration.isSessionCreationEnabled()) {
      throw new DisabledSessionException("Session creation for anonymous user disabled");
    }
    return subject.getSession(create);
  }

  @Override
  public void login(final AuthenticationToken token) throws AuthenticationException {
    subject.login(token);
    reset(); // if above throws, this will not get unset
  }

  @Override
  public void logout() {
    subject.logout();
    reset();
  }

  @Override
  public <V> V execute(final Callable<V> callable) throws ExecutionException {
    final Callable<V> associated = associateWith(callable);
    try {
      return associated.call();
    } catch (Throwable t) {
      throw new ExecutionException(t);
    }
  }

  @Override
  public void execute(final Runnable runnable) {
    final Runnable associated = associateWith(runnable);
    associated.run();
  }

  @Override
  public <V> Callable<V> associateWith(final Callable<V> callable) {
    return new SubjectCallable<V>(this, callable);
  }

  @Override
  public Runnable associateWith(final Runnable runnable) {
    if (runnable instanceof Thread) {
      String msg = "This implementation does not support Thread arguments because of JDK ThreadLocal " +
          "inheritance mechanisms required by Shiro.  Instead, the method argument should be a non-Thread " +
          "Runnable and the return value from this method can then be given to an ExecutorService or " +
          "another Thread.";
      throw new UnsupportedOperationException(msg);
    }
    return new SubjectRunnable(this, runnable);
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