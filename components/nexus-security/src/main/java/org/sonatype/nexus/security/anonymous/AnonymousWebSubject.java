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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.subject.WebSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Shiro Web subject of {@link AnonymousSubject}.
 */
public class AnonymousWebSubject
    extends AnonymousSubject
    implements WebSubject
{
  private final ServletRequest servletRequest;

  private final ServletResponse servletResponse;

  public AnonymousWebSubject(final ConfigurationFactory anonymousConfigurationSource,
                             final SecurityManager securityManager,
                             final PermissionResolver permissionResolver,
                             final Subject subject,
                             final ServletRequest servletRequest,
                             final ServletResponse servletResponse)
  {
    super(anonymousConfigurationSource, securityManager, permissionResolver, subject);
    this.servletRequest = checkNotNull(servletRequest);
    this.servletResponse = checkNotNull(servletResponse);
  }

  @Override
  public ServletRequest getServletRequest() {
    return servletRequest;
  }

  @Override
  public ServletResponse getServletResponse() {
    return servletResponse;
  }
}