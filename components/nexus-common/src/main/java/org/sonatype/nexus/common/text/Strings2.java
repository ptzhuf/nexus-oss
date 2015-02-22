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
package org.sonatype.nexus.common.text;

import com.google.common.base.Charsets;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * String helpers.
 *
 * @since 3.0
 */
public final class Strings2
{
  /**
   * Platform new-line separator.
   */
  @NonNls
  public static final String NL = System.getProperty("line.separator");

  /**
   * Password mask.
   */
  @NonNls
  public static final String MASK = "****";

  private Strings2() {}

  public static boolean isEmpty(final String value) {
    return value == null || value.trim().isEmpty();
  }

  public static boolean isNotEmpty(final String value) {
    return !isEmpty(value);
  }

  /**
   * Returns standard password {@link #MASK} for given value unless null.
   */
  @Nullable
  public static String mask(final @Nullable String password) {
    if (password != null) {
      return MASK;
    }
    return null;
  }

  /**
   * Converts bytes into a UTF-8 encoded string.
   */
  public static String utf8(final byte[] bytes) {
    return new String(bytes, Charsets.UTF_8);
  }

  /**
   * Converts a string into UTF-8 encoded bytes.
   */
  public static byte[] utf8(final String string) {
    return string.getBytes(Charsets.UTF_8);
  }

  /**
   * Encode separator into input at given delay.
   */
  public static String encode(final String input, final char separator, final int delay) {
    StringBuilder buff = new StringBuilder();

    int i = 0;
    for (char c : input.toCharArray()) {
      if (i != 0 && i % delay == 0) {
        buff.append(separator);
      }
      buff.append(c);
      i++;
    }

    return buff.toString();
  }
}
