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
package org.sonatype.nexus.proxy.maven.maven1;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.registry.AbstractIdContentClass;

/**
 * The Maven1 content class.
 *
 * @author cstamas
 * @deprecated To be removed once Maven1 support is removed.
 */
@Deprecated
@Named(Maven1ContentClass.ID)
@Singleton
public class Maven1ContentClass
    extends AbstractIdContentClass
{
  public static final String ID = "maven1";

  public static final String NAME = "Maven1";

  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return NAME;
  }
}
