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

import javax.annotation.Nullable;

/**
 * Validation response.
 */
public class ValidationResponse
{
  // FIXME: Remove unless there is good reason to keep this
  /**
   * A simple counter to enumerate messages.
   */
  private int key = 1;

  /**
   * A flag to mark is the config valid (usable) or not.
   */
  private boolean valid = true;

  // FIXME: Sort out why this is here and how its used, doesn't belong
  /**
   * A flag to mark is the config modified during validation or not.
   */
  private boolean modified = false;

  private List<ValidationMessage> validationErrors;

  private List<ValidationMessage> validationWarnings;

  private Object context;

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public boolean isModified() {
    return modified;
  }

  public void setModified(boolean modified) {
    this.modified = modified;
  }

  public List<ValidationMessage> getValidationErrors() {
    if (validationErrors == null) {
      validationErrors = new ArrayList<>();
    }
    return validationErrors;
  }

  public void addValidationError(final ValidationMessage message) {
    getValidationErrors().add(message);
    this.valid = false;
  }

  /**
   * @deprecated Avoid validation messages without keys!
   */
  @Deprecated
  public void addValidationError(final String message) {
    ValidationMessage e = new ValidationMessage(String.valueOf(key++), message);
    addValidationError(e);
  }

  public List<ValidationMessage> getValidationWarnings() {
    if (validationWarnings == null) {
      validationWarnings = new ArrayList<>();
    }
    return validationWarnings;
  }

  public void addValidationWarning(final ValidationMessage message) {
    getValidationWarnings().add(message);
  }

  /**
   * @deprecated Avoid validation messages without keys!
   */
  @Deprecated
  public void addValidationWarning(final String message) {
    ValidationMessage e = new ValidationMessage(String.valueOf(key++), message);
    addValidationWarning(e);
  }

  /**
   * A method to append a validation response to this validation response. The errors list and warnings list are
   * simply appended, and the isValid is logically AND-ed and isModified is logically OR-ed.
   */
  public void append(final ValidationResponse response) {
    for (ValidationMessage message : response.getValidationErrors()) {
      addValidationError(message);
    }

    for (ValidationMessage message : response.getValidationWarnings()) {
      addValidationWarning(message);
    }

    // FIXME: This is pointless, addValidationError() will set this flag as needed
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
