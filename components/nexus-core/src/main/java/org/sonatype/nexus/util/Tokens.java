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
package org.sonatype.nexus.util;

import org.sonatype.nexus.common.text.Strings2;

import com.google.common.io.BaseEncoding;

// TODO: Update to use Guava's BaseEncoding helpers and free from plexus-utils
// TODO: Update to use Guava's Hasher helpers and free from DigesterUtils
// TODO: Migrate to nexus-common once ^^^ is done

/**
 * Provides static methods for working with token-like thingies.
 *
 * @since 2.7
 * @deprecated Use {@link Strings2} instead.
 */
@Deprecated
public class Tokens
{
  // NOTE: guava apis decode to bytes, we want string input and output here
  public static String decodeBase64String(final String str) {
    return Strings2.utf8(BaseEncoding.base64().decode(str));
  }
}

