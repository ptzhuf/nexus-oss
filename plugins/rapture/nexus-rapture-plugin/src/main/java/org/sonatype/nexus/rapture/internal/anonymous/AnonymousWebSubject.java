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

import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.web.subject.WebSubject;

public class AnonymousWebSubject
    extends AnonymousSubject
    implements WebSubject
{
  private final WebSubject webSubject;

  public AnonymousWebSubject(
      final AnonymousConfiguration anonymousConfiguration,
      final Set<String> roles,
      final Set<Permission> permissions,
      final WebSubject webSubject)
  {
    super(anonymousConfiguration, roles, permissions, webSubject);
    this.webSubject = webSubject;
  }

  @Override
  public ServletRequest getServletRequest() {
    return webSubject.getServletRequest();
  }

  @Override
  public ServletResponse getServletResponse() {
    return webSubject.getServletResponse();
  }
}
