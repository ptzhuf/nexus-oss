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
package org.sonatype.nexus.configuration.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.ApplicationInterpolatorProvider;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Reader;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.codehaus.plexus.interpolation.InterpolatorFilterReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract class that encapsulates Modello model loading and saving with interpolation.
 *
 * @author cstamas
 */
public abstract class AbstractApplicationConfigurationSource
    extends ComponentSupport
    implements ApplicationConfigurationSource
{
  private final ApplicationInterpolatorProvider interpolatorProvider;

  private Configuration configuration;

  public AbstractApplicationConfigurationSource(final ApplicationInterpolatorProvider interpolatorProvider) {
    this.interpolatorProvider = checkNotNull(interpolatorProvider);
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  protected void loadConfiguration(final URL url) throws IOException, ConfigurationException {
    log.info("Loading configuration: {}", url);

    try (InputStream is = url.openStream()) {
      NexusConfigurationXpp3Reader reader = new NexusConfigurationXpp3Reader();
      InterpolatorFilterReader interpolator = new InterpolatorFilterReader(
          new InputStreamReader(is), interpolatorProvider.getInterpolator());
      configuration = reader.read(interpolator);
    }
    catch (XmlPullParserException e) {
      configuration = null;
      log.error("Invalid configuration: {}", url, e);
      throw new ConfigurationException("Invalid configuration: ", e);
    }

    if (!Configuration.MODEL_VERSION.equals(configuration.getVersion())) {
      String message = String.format("Invalid configuration; version mismatch expected=%s, found=%s",
          Configuration.MODEL_VERSION, configuration.getVersion());
      log.error(message);
      throw new ConfigurationException(message);
    }

    log.info("Loaded");
  }
}
