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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.apache.shiro.web.subject.WebSubject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Subject factory of anonymous enabled subjects.
 */
@Singleton
@Named
public class SubjectFactory
    extends DefaultWebSubjectFactory
{
  private final ConfigurationFactory configurationFactory;

  private final PermissionResolver permissionResolver;

  @Inject
  public SubjectFactory(final ConfigurationFactory configurationFactory,
                        final PermissionResolver permissionResolver)
  {
    this.configurationFactory = checkNotNull(configurationFactory);
    this.permissionResolver = checkNotNull(permissionResolver);
  }

  @Override
  public Subject createSubject(final SubjectContext context) {
    final Subject subject = super.createSubject(context);
    if (subject instanceof WebSubject) {
      final WebSubject webSubject = (WebSubject) subject;
      return new AnonymousWebSubject(configurationFactory, context.resolveSecurityManager(), permissionResolver,
          subject, webSubject.getServletRequest(), webSubject.getServletResponse());

    }
    else {
      return new AnonymousSubject(configurationFactory, context.resolveSecurityManager(), permissionResolver, subject);
    }
  }
}