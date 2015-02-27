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

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.InvalidCredentialsException;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * This is a facade around all things security ( authentication, authorization, user management, and configuration ).
 * It is meant to be the single point of access.
 *
 * @author Brian Demers
 */
public interface SecuritySystem
{
  /**
   * Starts the SecuritySystem. Before this method is called the state is unknown.
   */
  void start() throws Exception;

  /**
   * Stops the SecuritySystem. Provides a way to clean up resources.
   */
  void stop() throws Exception;

  // *********************
  // * authentication
  // *********************

  /**
   * Authenticates a user and logs them in. If successful returns a Subject.
   *
   * @return the Subject representing the logged in user.
   * @throws AuthenticationException if the user can not be authenticated
   */
  Subject login(AuthenticationToken token) throws AuthenticationException;

  /**
   * Authenticates a user and does NOT log them in. If successful returns a AuthenticationInfo.
   *
   * @return the AuthenticationInfo for this request.
   */
  AuthenticationInfo authenticate(AuthenticationToken token) throws AuthenticationException;

  /**
   * Finds the current logged in Subject.
   */
  Subject getSubject();

  /**
   * Logs the subject out.
   */
  void logout(Subject subject);

  // *********************
  // * authorization
  // *********************

  /**
   * Checks if principal has a permission.
   *
   * @return true only if the principal has the permission.
   */
  boolean isPermitted(PrincipalCollection principal, String permission);

  /**
   * Checks if principal has a list of permission.
   *
   * @param permissions list of permission to check.
   * @return A boolean array, the results in the array match the order of the permission
   */
  boolean[] isPermitted(PrincipalCollection principal, List<String> permissions);

  /**
   * Checks if principal has a permission, throws an AuthorizationException otherwise.
   */
  void checkPermission(PrincipalCollection principal, String permission) throws AuthorizationException;

  // ******************************
  // * Role permission management
  // ******************************

  /**
   * Lists all roles defined in the system. NOTE: this method could be slow if there is a large list of roles coming
   * from an external source (such as a database).
   *
   * @return All the roles defined in the system.
   */
  Set<Role> listRoles();

  /**
   * NOTE: this method could be slow if there is a large list of roles coming from an external source (such as a
   * database).
   *
   * @param sourceId The identifier of an {@link AuthorizationManager}.
   * @return All the roles defined by an {@link AuthorizationManager}.
   */
  Set<Role> listRoles(String sourceId) throws NoSuchAuthorizationManagerException;

  // *********************
  // * user management
  // *********************

  /**
   * Return the current user, if there is one, else null.
   */
  @Nullable
  User currentUser() throws UserNotFoundException;

  /**
   * Adds a new User to the system.<BR/>
   * Note: User.source must be set to specify where the user will be created.
   *
   * @param user     User to be created.
   * @param password The users initial password.
   * @return The User that was just created.
   */
  User addUser(User user, String password) throws NoSuchUserManagerException;

  /**
   * Get a User by id and source.
   *
   * @param userId   Id of the user to return.
   * @param sourceId the Id of the source to get the user from.
   * @return The user
   */
  User getUser(String userId, String sourceId) throws UserNotFoundException, NoSuchUserManagerException;

  /**
   * Get a User by id. This will search all sources (in order) looking for it. The first one found will be returned.
   * TODO: we should consider removing this in favor of its sibling that takes a source.
   *
   * @param userId Id of the user to return.
   * @return The user
   */
  User getUser(String userId) throws UserNotFoundException;

  /**
   * Updates a new User to the system.<BR/>
   * Note: User.source must be set to specify where the user will be modified.
   *
   * @param user User to be updated.
   * @return The User that was just updated.
   */
  User updateUser(User user) throws UserNotFoundException, NoSuchUserManagerException;

  /**
   * Remove a user based on the Id.
   *
   * @param userId The id of the user to be removed.
   * @Deprecated use deleteUser( String userId, String source )
   */
  @Deprecated
  void deleteUser(String userId) throws UserNotFoundException;

  /**
   * Removes a user based on the userId and sourceId.
   *
   * @param userId   The id of the user to be removed.
   * @param sourceId The sourceId of the user to be removed.
   */
  void deleteUser(String userId, String sourceId) throws UserNotFoundException, NoSuchUserManagerException;

  /**
   * Sets the list of roles a user has.
   *
   * @param userId          The id of the user.
   * @param sourceId        The sourceId where the user is located.
   * @param roleIdentifiers The list of roles to give the user.
   */
  void setUsersRoles(String userId, String sourceId, Set<RoleIdentifier> roleIdentifiers) throws UserNotFoundException;

  /**
   * Retrieve all Users . NOTE: This could be slow if there lots of users coming from external realms (a database).
   *
   * @deprecated use searchUsers.
   */
  @Deprecated
  Set<User> listUsers();

  /**
   * Searches for Users by criteria.
   */
  Set<User> searchUsers(UserSearchCriteria criteria);

  /**
   * Updates a users password.
   *
   * @param userId      The id of the user.
   * @param oldPassword The user's current password.
   * @param newPassword The user's new password.
   */
  void changePassword(String userId, String oldPassword, String newPassword)
      throws UserNotFoundException, InvalidCredentialsException;

  /**
   * Updates a users password. NOTE: This method does not require the old password to be known, it is meant for
   * administrators a users password.
   *
   * @param userId      The id of the user.
   * @param newPassword The user's new password.
   */
  void changePassword(String userId, String newPassword) throws UserNotFoundException;

  // *********************
  // * Authorization Management
  // *********************

  /**
   * List all privileges in the system.
   *
   * @return A set of all the privileges in the system.
   */
  Set<Privilege> listPrivileges();

  AuthorizationManager getAuthorizationManager(String source) throws NoSuchAuthorizationManagerException;

  // //
  // Application configuration, TODO: I don't think all of these need to be exposed, but they currently are
  // //

  /**
   * @deprecated use {@link RealmManager} instead.
   */
  @Deprecated
  List<String> getRealms();

  /**
   * @deprecated use {@link RealmManager} instead.
   */
  @Deprecated
  void setRealms(List<String> realms);

  /**
   * Returns the configured shiro SecurityManager
   */
  RealmSecurityManager getRealmSecurityManager();
}
