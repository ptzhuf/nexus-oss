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
package org.sonatype.nexus.internal.metrics;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customized {@link com.codahale.metrics.servlets.HealthCheckServlet} to support injection.
 *
 * @since 3.0
 *
 * @see HealthCheckMediator
 */
@Singleton
public class HealthCheckServlet
  extends com.codahale.metrics.servlets.HealthCheckServlet
{
  private static final Logger log = LoggerFactory.getLogger(HealthCheckServlet.class);

  @Inject
  public HealthCheckServlet(final HealthCheckRegistry registry, final Map<String,HealthCheck> healthChecks) {
    super(registry);

    // prime registry with existing health-check components
    for (Entry<String,HealthCheck> entry : healthChecks.entrySet()) {
      log.debug("Registering: {}", entry);
      registry.register(entry.getKey(), entry.getValue());
    }
  }
}
