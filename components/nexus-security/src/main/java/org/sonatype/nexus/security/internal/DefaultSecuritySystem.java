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

package org.sonatype.nexus.security.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.throwables.ConfigurationException;
import org.sonatype.nexus.security.SecurityConfigurationChanged;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.settings.SecuritySettingsManager;
import org.sonatype.nexus.security.user.InvalidCredentialsException;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.RoleMappingUserManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import net.sf.ehcache.CacheManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This implementation wraps a Shiro SecurityManager, and adds user management.
 */
@Named("default")
@Singleton
public class DefaultSecuritySystem
    extends ComponentSupport
    implements SecuritySystem
{
  private SecuritySettingsManager securitySettingsManager;

  private RealmSecurityManager securityManager;

  private CacheManager cacheManager;

  private Map<String, UserManager> userManagers;

  private Map<String, Realm> realmMap;

  private Map<String, AuthorizationManager> authorizationManagers;

  private EventBus eventBus;

  private static final String ALL_ROLES_KEY = "all";

  private volatile boolean started;

  @Inject
  public DefaultSecuritySystem(final EventBus eventBus,
                               final Map<String, AuthorizationManager> authorizationManagers,
                               final Map<String, Realm> realmMap,
                               final SecuritySettingsManager securitySettingsManager,
                               final RealmSecurityManager securityManager,
                               final CacheManager cacheManager,
                               final Map<String, UserManager> userManagers)
  {
    this.eventBus = eventBus;
    this.authorizationManagers = authorizationManagers;
    this.realmMap = realmMap;
    this.securitySettingsManager = securitySettingsManager;
    this.securityManager = securityManager;
    this.cacheManager = cacheManager;

    this.eventBus.register(this);
    this.userManagers = userManagers;
    SecurityUtils.setSecurityManager(getSecurityManager());
    started = false;
  }

  @Override
  public RealmSecurityManager getSecurityManager() {
    return securityManager;
  }

  @Override
  public Subject getSubject() {
    // this gets the currently bound Subject to the thread
    return SecurityUtils.getSubject();
  }

  @Override
  public synchronized void start() {
    if (started) {
      throw new IllegalStateException(getClass().getName()
          + " was already started, same instance is not re-startable!");
    }
    // reload the config
    securitySettingsManager.clearCache();

    // setup the CacheManager ( this could be injected if we where less coupled with ehcache)
    // The plexus wrapper can interpolate the config
    EhCacheManager ehCacheManager = new EhCacheManager();
    ehCacheManager.setCacheManager(cacheManager);
    getSecurityManager().setCacheManager(ehCacheManager);

    if (org.apache.shiro.util.Initializable.class.isInstance(getSecurityManager())) {
      ((org.apache.shiro.util.Initializable) getSecurityManager()).init();
    }
    setSecurityManagerRealms();
    started = true;
  }

  @Override
  public synchronized void stop() {
    if (getSecurityManager().getRealms() != null) {
      for (Realm realm : getSecurityManager().getRealms()) {
        if (AuthenticatingRealm.class.isInstance(realm)) {
          ((AuthenticatingRealm) realm).setAuthenticationCache(null);
        }
        if (AuthorizingRealm.class.isInstance(realm)) {
          ((AuthorizingRealm) realm).setAuthorizationCache(null);
        }
      }
    }

    // we need to kill caches on stop
    getSecurityManager().destroy();
    // cacheManagerComponent.shutdown();
  }

  @Override
  public Subject login(AuthenticationToken token) throws AuthenticationException {
    Subject subject = getSubject();
    subject.login(token);
    return subject;
  }

  @Override
  public AuthenticationInfo authenticate(AuthenticationToken token) throws AuthenticationException {
    return getSecurityManager().authenticate(token);
  }

  @Override
  public void logout(Subject subject) {
    subject.logout();
  }

  @Override
  public boolean isPermitted(PrincipalCollection principal, String permission) {
    return getSecurityManager().isPermitted(principal, permission);
  }

  @Override
  public boolean[] isPermitted(PrincipalCollection principal, List<String> permissions) {
    return getSecurityManager().isPermitted(principal, permissions.toArray(new String[permissions.size()]));
  }

  @Override
  public void checkPermission(PrincipalCollection principal, String permission) throws AuthorizationException {
    getSecurityManager().checkPermission(principal, permission);
  }

  @Override
  public Set<Role> listRoles() {
    Set<Role> roles = new HashSet<Role>();
    for (AuthorizationManager authzManager : authorizationManagers.values()) {
      Set<Role> tmpRoles = authzManager.listRoles();
      if (tmpRoles != null) {
        roles.addAll(tmpRoles);
      }
    }

    return roles;
  }

  @Override
  public Set<Role> listRoles(String sourceId) throws NoSuchAuthorizationManagerException {
    if (ALL_ROLES_KEY.equalsIgnoreCase(sourceId)) {
      return listRoles();
    }
    else {
      AuthorizationManager authzManager = getAuthorizationManager(sourceId);
      return authzManager.listRoles();
    }
  }

  @Override
  public Set<Privilege> listPrivileges() {
    Set<Privilege> privileges = new HashSet<Privilege>();
    for (AuthorizationManager authzManager : authorizationManagers.values()) {
      Set<Privilege> tmpPrivileges = authzManager.listPrivileges();
      if (tmpPrivileges != null) {
        privileges.addAll(tmpPrivileges);
      }
    }

    return privileges;
  }

  // *********************
  // * user management
  // *********************

  @Override
  public User addUser(User user, String password) throws NoSuchUserManagerException {
    // first save the user
    // this is the UserManager that owns the user
    UserManager userManager = getUserManager(user.getSource());

    if (!userManager.supportsWrite()) {
      throw new ConfigurationException("UserManager: " + userManager.getSource() + " does not support writing.");
    }

    userManager.addUser(user, password);

    // then save the users Roles
    for (UserManager tmpUserManager : getUserManagers()) {
      // skip the user manager that owns the user, we already did that
      // these user managers will only save roles
      if (!tmpUserManager.getSource().equals(user.getSource())
          && RoleMappingUserManager.class.isInstance(tmpUserManager)) {
        try {
          RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) tmpUserManager;
          roleMappingUserManager.setUsersRoles(user.getUserId(), user.getSource(),
              RoleIdentifier.getRoleIdentifiersForSource(user.getSource(),
                  user.getRoles()));
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the usermanager: {}",
              user.getUserId(), tmpUserManager.getSource());
        }
      }
    }

    return user;
  }

  @Override
  public User updateUser(User user) throws UserNotFoundException, NoSuchUserManagerException {
    // first update the user
    // this is the UserManager that owns the user
    UserManager userManager = getUserManager(user.getSource());

    if (!userManager.supportsWrite()) {
      throw new ConfigurationException("UserManager: " + userManager.getSource() + " does not support writing.");
    }

    final User oldUser = userManager.getUser(user.getUserId());
    userManager.updateUser(user);
    if (oldUser.getStatus() == UserStatus.active && user.getStatus() != oldUser.getStatus()) {
      // clear the realm authc caches as user got disabled
      eventBus.post(new UserPrincipalsExpired(user.getUserId(), user.getSource()));
    }

    // then save the users Roles
    for (UserManager tmpUserManager : getUserManagers()) {
      // skip the user manager that owns the user, we already did that
      // these user managers will only save roles
      if (!tmpUserManager.getSource().equals(user.getSource())
          && RoleMappingUserManager.class.isInstance(tmpUserManager)) {
        try {
          RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) tmpUserManager;
          roleMappingUserManager.setUsersRoles(user.getUserId(), user.getSource(),
              RoleIdentifier.getRoleIdentifiersForSource(user.getSource(),
                  user.getRoles()));
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the usermanager: {}",
              user.getUserId(), tmpUserManager.getSource());
        }
      }
    }

    // clear the realm authz caches as user might get roles changed
    eventBus.post(new AuthorizationConfigurationChanged());

    return user;
  }

  @Override
  public void deleteUser(String userId) throws UserNotFoundException {
    User user = getUser(userId);
    try {
      deleteUser(userId, user.getSource());
    }
    catch (NoSuchUserManagerException e) {
      log.error("User manager returned user, but could not be found: " + e.getMessage(), e);
      throw new IllegalStateException("User manager returned user, but could not be found: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteUser(String userId, String source) throws UserNotFoundException, NoSuchUserManagerException {
    checkNotNull(userId, "User ID may not be null");

    Subject subject = getSubject();
    if (subject != null && subject.getPrincipal() != null && userId.equals(subject.getPrincipal().toString())) {
      throw new IllegalArgumentException(
          "The user with user ID [" + userId
              + "] cannot be deleted, as that is the user currently logged into the application."
      );
    }

    if (isAnonymousAccessEnabled() && userId.equals(getAnonymousUsername())) {
      throw new IllegalArgumentException(
          "The user with user ID [" + userId
              + "] cannot be deleted, since it is marked user used for Anonymous access in Server Administration. "
              + "To delete this user, disable anonymous access or, "
              + "change the anonymous username and password to another valid values!"
      );
    }

    UserManager userManager = getUserManager(source);
    userManager.deleteUser(userId);

    // flush authc
    eventBus.post(new UserPrincipalsExpired(userId, source));
  }

  @Override
  public void setUsersRoles(String userId, String source, Set<RoleIdentifier> roleIdentifiers)
      throws UserNotFoundException
  {
    // TODO: this is a bit sticky, what we really want to do is just expose the RoleMappingUserManagers this way (i
    // think), maybe this is too generic

    boolean foundUser = false;

    for (UserManager tmpUserManager : getUserManagers()) {
      if (RoleMappingUserManager.class.isInstance(tmpUserManager)) {
        RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) tmpUserManager;
        try {
          foundUser = true;
          roleMappingUserManager.setUsersRoles(userId,
              source,
              RoleIdentifier.getRoleIdentifiersForSource(tmpUserManager.getSource(),
                  roleIdentifiers));
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the usermanager: {}",
              userId, tmpUserManager.getSource());
        }
      }
    }

    if (!foundUser) {
      throw new UserNotFoundException(userId);
    }
    // clear the authz realm caches
    eventBus.post(new AuthorizationConfigurationChanged());
  }

  private User findUser(String userId, UserManager userManager) throws UserNotFoundException {
    log.trace("Finding user: {} in user-manager: {}", userId, userManager);

    User user = userManager.getUser(userId);
    if (user == null) {
      throw new UserNotFoundException(userId);
    }
    log.trace("Found user: {}", user);

    // add roles from other user managers
    addOtherRolesToUser(user);

    return user;
  }

  @Override
  @Nullable
  public User currentUser() throws UserNotFoundException {
    Subject subject = getSubject();
    if (subject.getPrincipal() == null) {
      return null;
    }

    return getUser(subject.getPrincipal().toString());
  }

  @Override
  public User getUser(String userId) throws UserNotFoundException {
    log.trace("Finding user: {}", userId);

    for (UserManager userManager : orderUserManagers()) {
      try {
        return findUser(userId, userManager);
      }
      catch (UserNotFoundException e) {
        log.trace("User: '{}' was not found in: '{}'", userId, userManager, e);
      }
    }

    log.trace("User not found: {}", userId);
    throw new UserNotFoundException(userId);
  }

  @Override
  public User getUser(String userId, String source) throws UserNotFoundException, NoSuchUserManagerException {
    log.trace("Finding user: {} in source: {}", userId, source);

    UserManager userManager = getUserManager(source);
    return findUser(userId, userManager);
  }

  @Override
  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    for (UserManager tmpUserManager : getUserManagers()) {
      users.addAll(tmpUserManager.listUsers());
    }

    // now add all the roles to the users
    for (User user : users) {
      // add roles from other user managers
      addOtherRolesToUser(user);
    }

    return users;
  }

  @Override
  public Set<User> searchUsers(UserSearchCriteria criteria) {
    Set<User> users = new HashSet<User>();

    // if the source is not set search all realms.
    if (Strings2.isEmpty(criteria.getSource())) {
      // search all user managers
      for (UserManager tmpUserManager : getUserManagers()) {
        Set<User> result = tmpUserManager.searchUsers(criteria);
        if (result != null) {
          users.addAll(result);
        }
      }
    }
    else {
      try {
        users.addAll(getUserManager(criteria.getSource()).searchUsers(criteria));
      }
      catch (NoSuchUserManagerException e) {
        log.warn("UserManager: {} was not found.", criteria.getSource(), e);
      }
    }

    // now add all the roles to the users
    for (User user : users) {
      // add roles from other user managers
      addOtherRolesToUser(user);
    }

    return users;
  }

  /**
   * We need to order the UserManagers the same way as the Realms are ordered. We need to be able to find a user
   * based on the ID.
   *
   * This my never go away, but the current reason why we need it is:
   * https://issues.apache.org/jira/browse/KI-77 There is no (clean) way to resolve a realms roles into permissions.
   * take a look at the issue and VOTE!
   *
   * @return the list of UserManagers in the order (as close as possible) to the list of realms.
   */
  private List<UserManager> orderUserManagers() {
    List<UserManager> orderedLocators = new ArrayList<UserManager>();

    List<UserManager> unOrderdLocators = new ArrayList<UserManager>(getUserManagers());

    Map<String, UserManager> realmToUserManagerMap = new HashMap<String, UserManager>();

    for (UserManager userManager : getUserManagers()) {
      if (userManager.getAuthenticationRealmName() != null) {
        realmToUserManagerMap.put(userManager.getAuthenticationRealmName(), userManager);
      }
    }

    // get the sorted order of realms from the realm locator
    Collection<Realm> realms = getSecurityManager().getRealms();

    for (Realm realm : realms) {
      // now user the realm.name to find the UserManager
      if (realmToUserManagerMap.containsKey(realm.getName())) {
        UserManager userManager = realmToUserManagerMap.get(realm.getName());
        // remove from unorderd and add to orderd
        unOrderdLocators.remove(userManager);
        orderedLocators.add(userManager);
      }
    }

    // now add all the un-ordered ones to the ordered ones, this way they will be at the end of the ordered list
    orderedLocators.addAll(unOrderdLocators);

    return orderedLocators;
  }

  private void addOtherRolesToUser(User user) {
    // then save the users Roles
    for (UserManager tmpUserManager : getUserManagers()) {
      // skip the user manager that owns the user, we already did that
      // these user managers will only have roles
      if (!tmpUserManager.getSource().equals(user.getSource())
          && RoleMappingUserManager.class.isInstance(tmpUserManager)) {
        try {
          RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) tmpUserManager;
          Set<RoleIdentifier> roleIdentifiers =
              roleMappingUserManager.getUsersRoles(user.getUserId(), user.getSource());
          if (roleIdentifiers != null) {
            user.addAllRoles(roleIdentifiers);
          }
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the usermanager: {}",
              user.getUserId(), tmpUserManager.getSource());
        }
      }
    }
  }

  @Override
  public AuthorizationManager getAuthorizationManager(String source) throws NoSuchAuthorizationManagerException {
    if (!authorizationManagers.containsKey(source)) {
      throw new NoSuchAuthorizationManagerException("AuthorizationManager with source: '" + source
          + "' could not be found.");
    }

    return authorizationManagers.get(source);
  }

  @Override
  public void changePassword(String userId, String oldPassword, String newPassword)
      throws UserNotFoundException, InvalidCredentialsException
  {
    // first authenticate the user
    try {
      UsernamePasswordToken authenticationToken = new UsernamePasswordToken(userId, oldPassword);
      if (getSecurityManager().authenticate(authenticationToken) == null) {
        throw new InvalidCredentialsException();
      }
    }
    catch (AuthenticationException e) {
      log.debug("User failed to change password reason: " + e.getMessage(), e);
      throw new InvalidCredentialsException();
    }

    // if that was good just change the password
    changePassword(userId, newPassword);
  }

  @Override
  public void changePassword(String userId, String newPassword) throws UserNotFoundException {
    User user = getUser(userId);

    try {
      UserManager userManager = getUserManager(user.getSource());
      userManager.changePassword(userId, newPassword);
    }
    catch (NoSuchUserManagerException e) {
      // this should NEVER happen
      log.warn("User '{}' with source: '{}' but could not find the UserManager for that source.",
          userId, user.getSource());
    }

    // flush authc
    eventBus.post(new UserPrincipalsExpired(userId, user.getSource()));
  }

  private Collection<UserManager> getUserManagers() {
    return userManagers.values();
  }

  private UserManager getUserManager(final String source) throws NoSuchUserManagerException {
    if (!userManagers.containsKey(source)) {
      throw new NoSuchUserManagerException("UserManager with source: '" + source + "' could not be found.");
    }
    return userManagers.get(source);
  }

  // ==

  @Subscribe
  public void onEvent(final UserPrincipalsExpired event) {
    // TODO: we could do this better, not flushing whole cache for single user being deleted
    clearAuthcRealmCaches();
  }

  @Subscribe
  public void onEvent(final AuthorizationConfigurationChanged event) {
    // TODO: we could do this better, not flushing whole cache for single user roles being updated
    clearAuthzRealmCaches();
  }

  @Subscribe
  public void onEvent(final SecurityConfigurationChanged event) {
    clearAuthcRealmCaches();
    clearAuthzRealmCaches();
    securitySettingsManager.clearCache();
    setSecurityManagerRealms();
  }

  //
  // Realms
  //

  private void clearIfNonNull(@Nullable final Cache cache) {
    if (cache != null) {
      cache.clear();
    }
  }

  /**
   * Looks up registered {@link AuthenticatingRealm}s, and clears their authc caches if they have it set.
   */
  private void clearAuthcRealmCaches() {
    // NOTE: we don't need to iterate all the Sec Managers, they use the same Realms, so one is fine.
    final Collection<Realm> realms = getSecurityManager().getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthenticatingRealm) {
          clearIfNonNull(((AuthenticatingRealm) realm).getAuthenticationCache());
        }
      }
    }
  }

  /**
   * Looks up registered {@link AuthorizingRealm}s, and clears their authz caches if they have it set.
   */
  private void clearAuthzRealmCaches() {
    // NOTE: we don't need to iterate all the Sec Managers, they use the same Realms, so one is fine.
    final Collection<Realm> realms = getSecurityManager().getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthorizingRealm) {
          clearIfNonNull(((AuthorizingRealm) realm).getAuthorizationCache());
        }
      }
    }
  }

  private void setSecurityManagerRealms() {
    Collection<Realm> realms = getRealmsFromConfigSource();
    log.debug("Security manager realms: {}", realms);
    getSecurityManager().setRealms(Lists.newArrayList(realms));
  }

  private Collection<Realm> getRealmsFromConfigSource() {
    List<Realm> realms = new ArrayList<Realm>();

    List<String> realmIds = securitySettingsManager.getRealms();

    for (String realmId : realmIds) {
      if (realmMap.containsKey(realmId)) {
        realms.add(realmMap.get(realmId));
      }
      else {
        log.debug("Failed to look up realm as a component, trying reflection");
        // If that fails, will simply use reflection to load
        try {
          realms.add((Realm) getClass().getClassLoader().loadClass(realmId).newInstance());
        }
        catch (Exception e) {
          log.error("Unable to lookup security realms", e);
        }
      }
    }

    return realms;
  }

  @Override
  public List<String> getRealms() {
    return new ArrayList<String>(securitySettingsManager.getRealms());
  }

  @Override
  public void setRealms(List<String> realms) {
    securitySettingsManager.setRealms(realms);
    securitySettingsManager.save();

    // update the realms in the security manager
    setSecurityManagerRealms();
  }

  //
  // Anonymous
  //

  @Override
  public boolean isAnonymousAccessEnabled() {
    return securitySettingsManager.isAnonymousAccessEnabled();
  }

  private void setAnonymousAccessEnabled(boolean enabled) {
    securitySettingsManager.setAnonymousAccessEnabled(enabled);
    securitySettingsManager.save();
  }

  @Override
  public String getAnonymousUsername() {
    return securitySettingsManager.getAnonymousUsername();
  }

  private void setAnonymousUsername(String anonymousUsername) {
    User user = null;
    try {
      user = getUser(securitySettingsManager.getAnonymousUsername());
    }
    catch (UserNotFoundException e) {
      // ignore
    }
    securitySettingsManager.setAnonymousUsername(anonymousUsername);
    securitySettingsManager.save();
    // flush authc, if anon existed before change
    if (user != null) {
      eventBus.post(new UserPrincipalsExpired(user.getUserId(), user.getSource()));
    }
  }

  @Override
  public String getAnonymousPassword() {
    return securitySettingsManager.getAnonymousPassword();
  }

  private void setAnonymousPassword(String anonymousPassword) {
    User user = null;
    try {
      user = getUser(securitySettingsManager.getAnonymousUsername());
    }
    catch (UserNotFoundException e) {
      // ignore
    }
    securitySettingsManager.setAnonymousPassword(anonymousPassword);
    securitySettingsManager.save();
    if (user != null) {
      // flush authc, if anon exists
      eventBus.post(new UserPrincipalsExpired(user.getUserId(), user.getSource()));
    }
  }

  @Override
  public void setAnonymousAccess(final boolean enabled, final String username, final String password) {
    if (enabled) {
      if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
        throw new ConfigurationException("Anonymous access is getting enabled without valid username and/or password!");
      }

      final String oldUsername = getAnonymousUsername();
      final String oldPassword = getAnonymousPassword();

      // try to enable the "anonymous" user defined in XML realm, but ignore any problem (users might delete or
      // already disabled it, or completely removed XML realm) this is needed as below we will try a login
      final boolean statusChanged = setAnonymousUserEnabled(username, true);

      // detect change
      if (!Objects.equals(oldUsername, username) || !Objects.equals(oldPassword, password)) {
        try {
          // test authc with changed credentials
          try {
            // try to "log in" with supplied credentials
            // the anon user a) should exists
            getUser(username);
            // b) the pwd must work
            authenticate(new UsernamePasswordToken(username, password));
          }
          catch (UserNotFoundException e) {
            final String msg = "User \"" + username + "'\" does not exist.";
            log.warn("Nexus refused to apply configuration, the supplied anonymous information is wrong: " + msg, e);
            throw new ConfigurationException(msg, e);
          }
          catch (AuthenticationException e) {
            final String msg = "The password of user \"" + username + "\" is incorrect.";
            log.warn("Nexus refused to apply configuration, the supplied anonymous information is wrong: " + msg, e);
            throw new ConfigurationException(msg, e);
          }
        }
        catch (ConfigurationException e) {
          if (statusChanged) {
            setAnonymousUserEnabled(username, false);
          }
          throw e;
        }

        // set the changed username/pw
        setAnonymousUsername(username);
        setAnonymousPassword(password);
      }

      setAnonymousAccessEnabled(true);
    }
    else {
      // get existing username from XML realm, if we can (if security config about to be disabled still holds this
      // info)
      final String existingUsername = getAnonymousUsername();

      if (!Strings.isNullOrEmpty(existingUsername)) {
        // try to disable the "anonymous" user defined in XML realm, but ignore any problem (users might delete
        // or already disabled it, or completely removed XML realm)
        setAnonymousUserEnabled(existingUsername, false);
      }

      setAnonymousAccessEnabled(false);
    }

    // TODO: Save?  ATM relies on setAnonymousAccessEnabled() to save, pita
  }

  private boolean setAnonymousUserEnabled(final String anonymousUsername, final boolean enabled) {
    try {
      final User anonymousUser = getUser(anonymousUsername, UserManager.DEFAULT_SOURCE);
      final UserStatus oldStatus = anonymousUser.getStatus();
      if (enabled) {
        anonymousUser.setStatus(UserStatus.active);
      }
      else {
        anonymousUser.setStatus(UserStatus.disabled);
      }
      updateUser(anonymousUser);
      return !oldStatus.equals(anonymousUser.getStatus());
    }
    catch (UserNotFoundException e) {
      // ignore, anon user maybe manually deleted from XML realm by Nexus admin, is okay (kinda expected)
      log.debug("Anonymous user not found while trying to disable it (as part of disabling anonymous access)!", e);
      return false;
    }
    catch (NoSuchUserManagerException e) {
      // ignore, XML realm removed from configuration by Nexus admin, is okay (kinda expected)
      log.debug("XML Realm not found while trying to disable Anonymous user; as part of disabling anonymous access", e);
      return false;
    }
    catch (ConfigurationException e) {
      // do not ignore, and report, as this jeopardizes whole security functionality
      // we did not perform any _change_ against security sofar (we just did reading from it),
      // so it is okay to bail out at this point
      log.warn(
          "XML Realm reported invalid configuration while trying to disable Anonymous user (as part of disabling anonymous access)!",
          e);
      throw e;
    }
  }
}
