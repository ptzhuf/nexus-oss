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
package org.sonatype.configuration.validation;

import com.google.common.base.Throwables;

/**
 * Validation message.
 */
public class ValidationMessage
{
  private String key;

  private String message;

  // FIXME: Remove unused shortMessage and related
  private String shortMessage;

  private Throwable cause;

  public ValidationMessage(String key, String message) {
    this(key, message, (Throwable) null);
  }

  @Deprecated
  public ValidationMessage(String key, String message, String shortMessage) {
    this(key, message, shortMessage, null);
  }

  public ValidationMessage(String key, String message, Throwable cause) {
    this(key, message, message, cause);
  }

  @Deprecated
  public ValidationMessage(String key, String message, String shortMessage, Throwable cause) {
    this.key = key;
    this.message = message;
    this.shortMessage = shortMessage;
    this.cause = cause;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getShortMessage() {
    return shortMessage;
  }

  public void setShortMessage(String shortMessage) {
    this.shortMessage = shortMessage;
  }

  public Throwable getCause() {
    return cause;
  }

  public void setCause(Throwable cause) {
    this.cause = cause;
  }

  public String toString() {
    StringBuilder buff = new StringBuilder();

    buff.append(" o ").append(key).append(" - ").append(message);

    if (cause != null) {
      buff.append("\nCause:\n");
      buff.append(Throwables.getStackTraceAsString(cause));
    }

    return buff.toString();
  }
}
