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
package org.sonatype.nexus.security.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.validation.ValidationResponse;
import org.sonatype.nexus.security.settings.SecuritySettings;
import org.sonatype.nexus.security.settings.SecuritySettingsValidator;

@Named
@Singleton
public class SecuritySettingsValidatorImpl
    implements SecuritySettingsValidator
{
  public ValidationResponse validateModel(final SecuritySettings model) {
    ValidationResponse response = new ValidationResponse();
    response.setContext(model);
    response.append(validateRealms(model, model.getRealms()));
    return response;
  }

  public ValidationResponse validateRealms(final SecuritySettings model, List<String> realms) {
    ValidationResponse response = new ValidationResponse();
    response.setContext(model);

    if (model == null) {
      if (realms.size() < 1) {
        response.addValidationError("Security is enabled, You must have at least one realm enabled.");
      }
      // TODO: we should also try to load each one to see if it exists
    }

    return response;
  }
}
