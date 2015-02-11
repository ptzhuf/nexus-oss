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

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Anonymous token. To be used only with {@link AnonymousLoginFilter}.
 *
 * @since 3.0
 */
public class AnonymousToken
    implements AuthenticationToken
{
  public static final AnonymousToken TOKEN = new AnonymousToken();

  private static final Object PRINCIPAL = AnonymousRealm.PRINCIPAL;

  private static final Object CREDENTIALS = AnonymousRealm.CREDENTIALS;

  private AnonymousToken() {
  }

  @Override
  public Object getPrincipal() {
    return PRINCIPAL;
  }

  @Override
  public Object getCredentials() {
    return CREDENTIALS;
  }
}
