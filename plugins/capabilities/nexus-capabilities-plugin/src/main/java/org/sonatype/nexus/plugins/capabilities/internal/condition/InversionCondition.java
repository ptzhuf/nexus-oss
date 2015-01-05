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
package org.sonatype.nexus.plugins.capabilities.internal.condition;

import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.plugins.capabilities.support.condition.CompositeConditionSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

/**
 * A condition that applies a logical NOT on another condition.
 *
 * @since capabilities 2.0
 */
public class InversionCondition
    extends CompositeConditionSupport
    implements Condition
{

  private final Condition condition;

  public InversionCondition(final EventBus eventBus,
                            final Condition condition)
  {
    super(eventBus, condition);
    this.condition = condition;
  }

  @Override
  protected boolean reevaluate(final Condition... conditions) {
    return !conditions[0].isSatisfied();
  }

  @Override
  public String toString() {
    return "NOT " + condition;
  }

  @Override
  public String explainSatisfied() {
    return condition.explainUnsatisfied();
  }

  @Override
  public String explainUnsatisfied() {
    return condition.explainSatisfied();
  }
}
