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
package org.sonatype.nexus.common.validation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Validation response.
 */
public class ValidationResponse
{
  // FIXME: Remove, this is only used for deprecated non-message based add methods
  private int key = 1;

  private boolean valid = true;

  // FIXME: Sort out why this is here and how its used, doesn't belong
  // FIXME: This seems to be due to validation handling making changes to the models
  /**
   * A flag to mark is the config modified during validation or not.
   */
  private boolean modified = false;

  private List<ValidationMessage> errors;

  private List<ValidationMessage> warnings;

  private Object context;

  public boolean isValid() {
    return valid;
  }

  public void setValid(final boolean valid) {
    this.valid = valid;
  }

  public boolean isModified() {
    return modified;
  }

  public void setModified(final boolean modified) {
    this.modified = modified;
  }

  @Nonnull
  public List<ValidationMessage> getErrors() {
    if (errors == null) {
      errors = new ArrayList<>();
    }
    return errors;
  }

  public void addError(final ValidationMessage message) {
    checkNotNull(message);
    getErrors().add(message);
    valid = false;
  }

  /**
   * @deprecated Avoid validation messages without keys!
   */
  @Deprecated
  public void addError(final String message) {
    addError(new ValidationMessage(String.valueOf(key++), message));
  }

  @Nonnull
  public List<ValidationMessage> getWarnings() {
    if (warnings == null) {
      warnings = new ArrayList<>();
    }
    return warnings;
  }

  public void addWarning(final ValidationMessage message) {
    checkNotNull(message);
    getWarnings().add(message);
  }

  /**
   * @deprecated Avoid validation messages without keys!
   */
  @Deprecated
  public void addWarning(final String message) {
    addWarning(new ValidationMessage(String.valueOf(key++), message));
  }

  /**
   * A method to append a validation response to this validation response. The errors list and warnings list are
   * simply appended, and the isValid is logically AND-ed and isModified is logically OR-ed.
   */
  public void append(final ValidationResponse response) {
    for (ValidationMessage message : response.getErrors()) {
      addError(message);
    }
    for (ValidationMessage message : response.getWarnings()) {
      addWarning(message);
    }

    // FIXME: This is pointless, addError() will set this flag as needed
    setValid(isValid() && response.isValid());
    setModified(isModified() || response.isModified());
  }

  public void setContext(final @Nullable Object context) {
    this.context = context;
  }

  @Nullable
  public Object getContext() {
    return context;
  }
}
