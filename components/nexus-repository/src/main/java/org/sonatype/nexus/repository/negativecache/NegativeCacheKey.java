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
package org.sonatype.nexus.repository.negativecache;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A key for the {@link NegativeCacheFacet} not found cache.
 *
 * @since 3.0
 */
public class NegativeCacheKey
{
  private final String key;

  public NegativeCacheKey(final String key) {
    this.key = checkNotNull(key);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NegativeCacheKey that = (NegativeCacheKey) o;

    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "key='" + key + '\'' +
        '}';
  }
}
