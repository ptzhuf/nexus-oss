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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * @deprecated Use guava helpers instead.
 */
@Deprecated
public class DigesterUtils
{
  /**
   * Apply input-stream bytes to hash-function.
   */
  private static HashFunction apply(final InputStream input, final HashFunction function) throws IOException {
    byte[] buff = new byte[1024];
    int read;
    do {
      read = input.read(buff);
      function.hashBytes(buff, 0, read);
    }
    while (read != -1);
    return function;
  }

  public static String getSha256Digest(final InputStream input) {
    try {
      return apply(input, Hashing.sha256()).toString();
    }
    catch (IOException e) {
      return null;
    }
  }

  public static String getSha1Digest(final String content) {
    return Hashing.sha1().hashString(content, Charsets.UTF_8).toString();
  }

  public static String getSha1Digest(final InputStream input) {
    try {
      return apply(input, Hashing.sha1()).toString();
    }
    catch (IOException e) {
      return null;
    }
  }

  public static String getSha1Digest(final File file) {
    try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
      return apply(input, Hashing.sha1()).toString();
    }
    catch (IOException e) {
      return null;
    }
  }

  public static String getMd5Digest(final byte[] input) {
    return Hashing.md5().hashBytes(input).toString();
  }

  public static String getSha1Digest(final byte[] input) {
    return Hashing.sha1().hashBytes(input).toString();
  }
}
