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
package org.sonatype.nexus.rest.feeds.sources;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.feeds.SystemEvent;

/**
 * The system changes feed.
 *
 * @author cstamas
 */
@Named(SystemFeedSource.CHANNEL_KEY)
@Singleton
public class SystemFeedSource
    extends AbstractSystemFeedSource
{
  public static final String CHANNEL_KEY = "systemChanges";

  public List<SystemEvent> getEventList(Integer from, Integer count, Map<String, String> params) {
    return getFeedRecorder().getSystemEvents(null, from, count, null);
  }

  public String getFeedKey() {
    return CHANNEL_KEY;
  }

  public String getFeedName() {
    return getDescription();
  }

  @Override
  public String getDescription() {
    return "System changes in Nexus.";
  }

  @Override
  public String getTitle() {
    return "System changes";
  }

}
