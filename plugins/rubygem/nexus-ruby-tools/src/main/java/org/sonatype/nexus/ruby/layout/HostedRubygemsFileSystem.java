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
package org.sonatype.nexus.ruby.layout;

import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;

/**
 * this class assembles the hosted repository for GET, POST and DELETE request.
 *
 * @author christian
 */
public class HostedRubygemsFileSystem
    extends DefaultRubygemsFileSystem
{
  public HostedRubygemsFileSystem(RubygemsGateway gateway, Storage store) {
    super(new DefaultLayout(),
        new HostedGETLayout(gateway, store),
        new HostedPOSTLayout(gateway, store),
        new HostedDELETELayout(gateway, store));
  }
}