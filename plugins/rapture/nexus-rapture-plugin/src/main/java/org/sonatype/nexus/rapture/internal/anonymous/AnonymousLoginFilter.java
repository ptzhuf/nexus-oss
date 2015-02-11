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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.common.base.Throwables;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.AdviceFilter;

/**
 * Anonymous filter, that should be LAST in the filter chain for paths allowing anonymous users.
 * Note: "anonymous" user is NOT "Shiro guest"! It performs log-in for anonymous user. Does not fiddle with subject
 * instance, is "clean".
 *
 * @since 3.0
 */
@Named
@Singleton
public class AnonymousLoginFilter
    extends AdviceFilter
{
  private static final String ANONYMOUS_LOGGED_IN = AnonymousFilter.class.getName() + ".anonymousLoggedIn";

  @Override
  protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
    final Subject subject = SecurityUtils.getSubject();
    // we filter out subjects w/o principals, rememberMe subjects has principal but isAuthenticated=false
    if (subject.getPrincipal() == null) {
      try {
        subject.login(AnonymousToken.TOKEN);
        request.setAttribute(ANONYMOUS_LOGGED_IN, Boolean.TRUE);
      }
      catch (AuthenticationException e) {
        // what here? anon user disabled?
        throw Throwables.propagate(e);
      }
    }
    return true;
  }

  @Override
  public void afterCompletion(ServletRequest request, ServletResponse response, Exception exception) throws Exception {
    if (request.getAttribute(ANONYMOUS_LOGGED_IN) != null) {
      SecurityUtils.getSubject().logout();
    }
  }
}
