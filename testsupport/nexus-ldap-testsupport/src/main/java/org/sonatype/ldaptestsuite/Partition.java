/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.ldaptestsuite;

import java.io.File;
import java.util.List;

public class Partition
{

  private String name;

  private String suffix;

  private List<String> indexedAttributes;

  private List<String> rootEntryClasses;

  private File ldifFile;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public List<String> getIndexedAttributes() {
    return indexedAttributes;
  }

  public void setIndexedAttributes(List<String> indexedAttributes) {
    this.indexedAttributes = indexedAttributes;
  }

  public List<String> getRootEntryClasses() {
    return rootEntryClasses;
  }

  public void setRootEntryClasses(List<String> rootClasses) {
    this.rootEntryClasses = rootClasses;
  }

  public File getLdifFile() {
    return ldifFile;
  }

  public void setLdifFile(File ldifFile) {
    this.ldifFile = ldifFile;
  }

}
