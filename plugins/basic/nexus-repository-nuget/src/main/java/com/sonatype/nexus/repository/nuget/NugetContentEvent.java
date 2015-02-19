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
package com.sonatype.nexus.repository.nuget;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public abstract class NugetContentEvent
{
  private final String id;

  private final String version;

  private final OrientVertex component;
  
  public NugetContentEvent(final String id, final String version, final OrientVertex component) {
    this.id = id;
    this.version = version;
    this.component = component;
  }

  public final String getId() {
    return id;
  }

  public final String getVersion() {
    return version;
  }

  public final OrientVertex getComponent() {
    return component;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() +"{" +
        "id='" + id + '\'' +
        ", version='" + version + '\'' +
        ", component=" + component +
        '}';
  }
}
