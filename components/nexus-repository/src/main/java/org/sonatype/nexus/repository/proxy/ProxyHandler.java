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
package org.sonatype.nexus.repository.proxy;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * A format-neutral proxy handler.
 *
 * @since 3.0
 */
public class ProxyHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {

    final String action = context.getRequest().getAction();
    if (!HttpMethods.GET.equals(action)) {
      return HttpResponses.methodNotAllowed(action, GET);
    }

    final Payload payload = proxyFacet(context).get(context);

    if (payload != null) {
      return HttpResponses.ok(payload);
    }

    return HttpResponses.notFound();
  }

  private ProxyFacet proxyFacet(final Context context) {
    return context.getRepository().facet(ProxyFacet.class);
  }
}
