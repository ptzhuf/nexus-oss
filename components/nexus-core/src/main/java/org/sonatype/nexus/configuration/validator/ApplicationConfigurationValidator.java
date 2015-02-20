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
package org.sonatype.nexus.configuration.validator;

import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.model.CHttpProxySettings;
import org.sonatype.nexus.configuration.model.CPathMappingItem;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteNexusInstance;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryGrouping;
import org.sonatype.nexus.configuration.model.CRepositoryTarget;
import org.sonatype.nexus.configuration.model.CRestApiSettings;
import org.sonatype.nexus.configuration.model.CRouting;
import org.sonatype.nexus.configuration.model.CSmtpConfiguration;
import org.sonatype.nexus.configuration.model.Configuration;

/**
 * The validator used to validate current configuration in boot-up sequence.
 */
public interface ApplicationConfigurationValidator
{
  ValidationResponse validateModel(ValidationRequest<Configuration> request);

  /**
   * Validates a repository configuration.
   */
  ValidationResponse validateRepository(ApplicationValidationContext ctx, CRepository repository);

  /**
   * Validates remote connection settings.
   */
  ValidationResponse validateRemoteConnectionSettings(ApplicationValidationContext ctx,
                                                      CRemoteConnectionSettings settings);

  /**
   * Validates remote proxy settings.
   */
  ValidationResponse validateRemoteHttpProxySettings(ApplicationValidationContext ctx,
                                                     CRemoteHttpProxySettings settings);

  /**
   * Validates remote authentication.
   */
  ValidationResponse validateRemoteAuthentication(ApplicationValidationContext ctx, CRemoteAuthentication settings);

  /**
   * Validates rest api settings.
   */
  ValidationResponse validateRestApiSettings(ApplicationValidationContext ctx, CRestApiSettings settings);

  /**
   * Validates Nexus built-in HTTP proxy settings.
   */
  ValidationResponse validateHttpProxySettings(ApplicationValidationContext ctx, CHttpProxySettings settings);

  /**
   * Validates routing.
   */
  ValidationResponse validateRouting(ApplicationValidationContext ctx, CRouting settings);

  /**
   * Validates remote nexus instance.
   */
  ValidationResponse validateRemoteNexusInstance(ApplicationValidationContext ctx, CRemoteNexusInstance settings);

  /**
   * Validates repository grouping.
   */
  ValidationResponse validateRepositoryGrouping(ApplicationValidationContext ctx, CRepositoryGrouping settings);

  /**
   * Validates mapping.
   */
  ValidationResponse validateGroupsSettingPathMappingItem(ApplicationValidationContext ctx, CPathMappingItem settings);

  /**
   * Validates repository target item.
   */
  ValidationResponse validateRepositoryTarget(ApplicationValidationContext ctx, CRepositoryTarget settings);

  /**
   * Validates smtp
   */
  ValidationResponse validateSmtpConfiguration(ApplicationValidationContext ctx, CSmtpConfiguration settings);
}
