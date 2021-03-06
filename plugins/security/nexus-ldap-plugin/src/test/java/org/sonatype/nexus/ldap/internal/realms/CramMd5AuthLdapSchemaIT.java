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
package org.sonatype.nexus.ldap.internal.realms;


import org.sonatype.ldaptestsuite.LdapServer;
import org.sonatype.ldaptestsuite.LdapServerConfiguration;
import org.sonatype.nexus.ldap.internal.persist.entity.LdapConfiguration;

public class CramMd5AuthLdapSchemaIT
    extends LdapSchemaTestSupport
{
  @Override
  protected LdapServerConfiguration createServerConfiguration(final String name) {
    return LdapServerConfiguration.builder()
        .withWorkingDirectory(util.createTempDir())
        .withPartitions(createPartition(name))
        .withSasl("localhost", "ldap/localhost@EXAMPLE.COM", "ou=system", "localhost")
        .build();
  }

  @Override
  protected LdapConfiguration createLdapClientConfigurationForServer(final String name, final int order,
                                                                     final LdapServer ldapServer)
  {
    final LdapConfiguration ldapConfiguration = super.createLdapClientConfigurationForServer(name, order, ldapServer);
    ldapConfiguration.getConnection().setAuthScheme("CRAM-MD5");
    ldapConfiguration.getConnection().setSaslRealm("localhost");
    ldapConfiguration.getConnection().setSystemUsername("admin");
    ldapConfiguration.getConnection().setSystemPassword("secret");
    return ldapConfiguration;
  }
}
