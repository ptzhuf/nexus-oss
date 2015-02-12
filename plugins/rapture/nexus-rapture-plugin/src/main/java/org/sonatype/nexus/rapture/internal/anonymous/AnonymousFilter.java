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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.apache.shiro.web.subject.support.WebDelegatingSubject;

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
  public static final String NAME = "anonymous";

  private static final String ORIGINAL_SUBJECT = AnonymousFilter.class.getName() + ".originalSubject";

  private Subject anonymousSubject(final ServletRequest request, final ServletResponse response) {
    // todo: not using Subhect.builder as override is happening here as safety net
    final WebDelegatingSubject result = new WebDelegatingSubject(
        AnonymousRealm.PRINCIPAL_COLLECTION,
        false /*authenticated*/,
        request.getRemoteHost(),
        null/*session*/,
        false /*sessionCreationEnabled*/,
        request,
        response,
        SecurityUtils.getSecurityManager() // inject maybe
    )
    {
      // TODO: this override here should go away, is just a safety net for now
      @Override
      public void login(final AuthenticationToken token) throws AuthenticationException {
        throw new RuntimeException(getClass().getSimpleName() + " filter misplaced, should be last of authc filters");
      }
    };
    return result;
  }

  @Override
  protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
    final Subject subject = SecurityUtils.getSubject();
    // we filter out subjects w/o principals, rememberMe subjects has principal but isAuthenticated=false
    if (subject.getPrincipal() == null) {
      request.setAttribute(ORIGINAL_SUBJECT, subject);
      ThreadContext.bind(anonymousSubject(request, response));
    }
    return true;
  }

  @Override
  public void afterCompletion(ServletRequest request, ServletResponse response, Exception exception) throws Exception {
    final Subject originalSubject = (Subject) request.getAttribute(ORIGINAL_SUBJECT);
    if (originalSubject != null) {
      ThreadContext.bind(originalSubject);
    }
    else {
      ThreadContext.unbindSubject();
    }
  }
}
