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
package com.sonatype.nexus.repository.nuget.internal.odata;

import java.net.URI;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.junit.Test;

/**
 * @since 3.0
 */
public class DeleteMeTest
{
  @Test
  public void foo() throws Exception{

    final URI repoUri = new URI("http://www.nuget.org/api/v2");

    final URI queryUri = new URI("count?foo=a&b=c");

    final URI resolve = repoUri.resolve(queryUri);



    System.err.println(resolve);
  }

  @Test
  public void foo2() throws Exception{
    URIBuilder n = new URIBuilder();
    n.setPath("Packages()/$count");
    n.addParameter("jody","&%@");

    System.err.println(n.build());
  }

}
