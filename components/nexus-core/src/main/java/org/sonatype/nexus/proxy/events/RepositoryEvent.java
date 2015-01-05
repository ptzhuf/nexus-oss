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
package org.sonatype.nexus.proxy.events;

import org.sonatype.nexus.events.AbstractEvent;
import org.sonatype.nexus.proxy.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The event that is occurred within a Repository, such as content changes or other maintenance stuff.
 *
 * @author cstamas
 */
public abstract class RepositoryEvent
    extends AbstractEvent<Repository>
{
  public RepositoryEvent(final Repository repository) {
    super(checkNotNull(repository));
  }

  /**
   * Gets the repository.
   *
   * @return the repository
   */
  public Repository getRepository() {
    return getEventSender();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "repositoryId=" + getRepository().getId() +
        '}';
  }
}
