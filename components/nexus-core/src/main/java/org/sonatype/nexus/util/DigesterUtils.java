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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

// TODO: Update to use Guava's Hasher helpers and remove this ancient helper.

/**
 * A util class to calculate various digests on Strings. Usaful for some simple password management.
 */
@Deprecated
public class DigesterUtils
{
  public static String getDigest(String alg, InputStream is) throws NoSuchAlgorithmException {
    String result = null;

    try {
      try {
        byte[] buffer = new byte[1024];
        MessageDigest md = MessageDigest.getInstance(alg);

        int numRead;
        do {
          numRead = is.read(buffer);
          if (numRead > 0) {
            md.update(buffer, 0, numRead);
          }
        }
        while (numRead != -1);

        result = BaseEncoding.base16().lowerCase().encode(md.digest());
      }
      finally {
        is.close();
      }
    }
    catch (IOException e) {
      // hrm
      result = null;
    }

    return result;
  }

  public static String getSha1Digest(String content) {
    return Hashing.sha1().hashString(content, Charsets.UTF_8).toString();
  }

  public static String getSha1Digest(InputStream is) {
    try {
      return getDigest("SHA1", is);
    }
    catch (NoSuchAlgorithmException e) {
      // will not happen
      return null;
    }
  }

  public static String getSha1Digest(File file) {
    try {
      try (FileInputStream fis = new FileInputStream(file)) {
        return getDigest("SHA1", fis);
      }
      catch (NoSuchAlgorithmException e) {
        // will not happen
        return null;
      }
    }
    catch (IOException e) {
      return null;
    }
  }
  public static String getMd5Digest(byte[] input) {
    return Hashing.md5().hashBytes(input).toString();
  }

  public static String getSha1Digest(byte[] input) {
    return Hashing.sha1().hashBytes(input).toString();
  }
}
