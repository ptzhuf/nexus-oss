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

import org.codehaus.plexus.util.Base64;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

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
  @NonNls
  public static final String NL = Strings2.NL;

  public static String encode(final String input, final char separator, final int delay) {
    return Strings2.encode(input, separator, delay);
  }

  public static String string(final byte[] bytes) {
    return Strings2.utf8(bytes);
  }

  public static byte[] bytes(final String string) {
    return Strings2.utf8(string);
  }

  public static String mask(final @Nullable String password) {
    return Strings2.mask(password);
  }

  public static boolean isEmpty(final @Nullable String value) {
    return Strings2.isEmpty(value);
  }

  // FIXME: Depends on factoring out DigesterUtils and Base64

  public static String encodeHexString(final byte[] bytes) {
    return new String(DigesterUtils.encodeHex(bytes));
  }

  public static String encodeBase64String(final byte[] bytes) {
    return Strings2.utf8(Base64.encodeBase64(bytes));
  }

  public static byte[] decodeBase64(final byte[] bytes) {
    return Base64.decodeBase64(bytes);
  }

  public static String decodeBase64String(final String str) {
    return Strings2.utf8(Base64.decodeBase64(bytes(str)));
  }
}

