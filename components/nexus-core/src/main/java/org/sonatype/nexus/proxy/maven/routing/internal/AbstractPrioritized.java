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
package org.sonatype.nexus.proxy.maven.routing.internal;

import org.sonatype.nexus.proxy.maven.routing.discovery.Prioritized;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * Abstract class for {@link Prioritized} implementations.
 *
 * @author cstamas
 */
public abstract class AbstractPrioritized
    extends ComponentSupport
    implements Prioritized
{
  private final int priority;

  protected AbstractPrioritized(final int priority) {
    this.priority = priority;
  }

  @Override
  public int getPriority() {
    return priority;
  }
}
