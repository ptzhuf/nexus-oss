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
package org.sonatype.nexus.kenai.internal;

import java.util.Collections;

import org.sonatype.nexus.configuration.ApplicationConfiguration;
import org.sonatype.nexus.kenai.AbstractKenaiRealmTest;
import org.sonatype.nexus.security.SecuritySystem;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.junit.Test;

public class KenaiClearCacheTest
    extends AbstractKenaiRealmTest
{
  protected SecuritySystem securitySystem;

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    mockKenai();

    // to start the hobelevanc and make it use Kenai realm
    startNx();
    lookup(SecuritySystem.class).setRealms(Collections.singletonList("kenai"));
    lookup(ApplicationConfiguration.class).saveConfiguration();

    securitySystem = lookup(SecuritySystem.class);
  }


  @Test
  public void testClearCache()
      throws Exception
  {
    // so here is the problem, we clear the authz cache when ever config changes happen

    // now log the user in
    Subject subject1 = securitySystem.login(new UsernamePasswordToken(username, password));
    // check authz
    subject1.checkRole(DEFAULT_ROLE);

    // clear the cache
    KenaiRealm realm = (KenaiRealm) this.lookup(Realm.class, "kenai");
    realm.getAuthorizationCache().clear();

    // user should still have the role
    subject1.checkRole(DEFAULT_ROLE);

    // the user should be able to login again as well
    Subject subject2 = securitySystem.login(new UsernamePasswordToken(username, password));
    subject2.checkRole(DEFAULT_ROLE);
  }
}
