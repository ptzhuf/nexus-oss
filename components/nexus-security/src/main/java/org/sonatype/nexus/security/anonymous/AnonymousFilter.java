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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.AdviceFilter;

/**
 * Shiro servlet filter for Anonymous. It simply sets anonymous on enter, and unset it on exit.
 */
@Singleton
@Named
public class AnonymousFilter
    extends AdviceFilter
{
  public static final String NAME = "nx-anonymous";

  @Override
  protected boolean preHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    final Subject subject = SecurityUtils.getSubject();
    if (subject instanceof AnonymousSubject) {
      ((AnonymousSubject) subject).setAnonymous();
      return true;
    }
    else {
      // TODO: factory not set?
      throw new IllegalStateException(
          "SubjectFactory not set? Subject class not supported " + subject.getClass().getSimpleName());
    }
  }

  public void afterCompletion(final ServletRequest request, final ServletResponse response, final Exception exception)
      throws Exception
  {
    final Subject subject = SecurityUtils.getSubject();
    if (subject instanceof AnonymousSubject) {
      ((AnonymousSubject) subject).unsetAnonymous();
    }
    else {
      // TODO: something rebound/override subject?
      throw new IllegalStateException(
          "SubjectFactory not set? Subject class not supported " + subject.getClass().getSimpleName());
    }
  }
}