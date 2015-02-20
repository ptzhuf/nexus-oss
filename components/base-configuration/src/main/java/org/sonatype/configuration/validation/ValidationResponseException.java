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

import java.io.StringWriter;

/**
 * Validation response exception.
 *
 * @since 3.0
 */
public class ValidationResponseException
    extends RuntimeException // javax.validation.ValidationException?
{
  private ValidationResponse response;

  public ValidationResponseException(final ValidationResponse response) {
    this.response = response;
  }

  public ValidationResponse getResponse() {
    return response;
  }

  public String getMessage() {
    StringWriter buff = new StringWriter();

    // FIXME: Clean this mess up... :-(

    buff.append(super.getMessage());

    if (getResponse() != null) {
      if (getResponse().getValidationErrors().size() > 0) {
        buff.append("\nValidation errors follows:\n");

        for (ValidationMessage error : getResponse().getValidationErrors()) {
          buff.append(error.toString());
        }
        buff.append("\n");
      }

      if (getResponse().getValidationWarnings().size() > 0) {
        buff.append("\nValidation warnings follows:\n");

        for (ValidationMessage warning : getResponse().getValidationWarnings()) {
          buff.append(warning.toString());
        }
        buff.append("\n");
      }
    }

    return buff.toString();
  }
}
