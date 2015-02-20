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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.common.io.DirSupport;
import org.sonatype.nexus.configuration.ApplicationInterpolatorProvider;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.model.ConfigurationHelper;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Writer;
import org.sonatype.nexus.configuration.validator.ApplicationConfigurationValidator;
import org.sonatype.nexus.configuration.validator.ConfigurationValidator;
import org.sonatype.sisu.goodies.common.io.FileReplacer;
import org.sonatype.sisu.goodies.common.io.FileReplacer.ContentWriter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default configuration source powered by Modello. It will try to load configuration, upgrade if needed and
 * validate it. It also holds the one and only existing Configuration object.
 *
 * @author cstamas
 */
@Singleton
@Named("file")
public class FileConfigurationSource
    extends AbstractApplicationConfigurationSource
{
  private final Provider<SystemStatus> systemStatusProvider;

  private final File configurationFile;

  private final ApplicationConfigurationValidator configurationValidator;

  private final ApplicationConfigurationSource nexusDefaults;

  private final ConfigurationHelper configHelper;

  @Inject
  public FileConfigurationSource(final ApplicationInterpolatorProvider interpolatorProvider,
                                 final Provider<SystemStatus> systemStatusProvider,
                                 final @Named("${nexus-work}/etc/nexus.xml") File configurationFile,
                                 final ApplicationConfigurationValidator configurationValidator,
                                 final @Named("static") ApplicationConfigurationSource nexusDefaults,
                                 final ConfigurationHelper configHelper)
  {
    super(interpolatorProvider);
    this.systemStatusProvider = checkNotNull(systemStatusProvider);
    this.configurationFile = checkNotNull(configurationFile);
    this.configurationValidator = checkNotNull(configurationValidator);
    this.nexusDefaults = checkNotNull(nexusDefaults);
    this.configHelper = checkNotNull(configHelper);
  }

  private ConfigurationValidator getConfigurationValidator() {
    return configurationValidator;
  }

  private File getConfigurationFile() {
    return configurationFile;
  }

  @Override
  public Configuration loadConfiguration() throws ConfigurationException, IOException {
    // propagate call and fill in defaults too
    nexusDefaults.loadConfiguration();

    if (getConfigurationFile() == null || getConfigurationFile().getAbsolutePath().contains("${")) {
      throw new ConfigurationException("The configuration file is not set or resolved properly: "
          + getConfigurationFile().getAbsolutePath());
    }

    if (!getConfigurationFile().exists()) {
      log.warn("No configuration file in place, copying the default one and continuing with it.");

      // get the defaults and stick it to place
      setConfiguration(nexusDefaults.getConfiguration());
      saveConfiguration(getConfigurationFile());
    }

    loadConfiguration(getConfigurationFile());

    upgradeNexusVersion();

    ValidationResponse vResponse = getConfigurationValidator().validateModel(new ValidationRequest(getConfiguration()));
    dumpValidationErrors(vResponse);
    if (vResponse.isValid()) {
      if (vResponse.isModified()) {
        log.info("Validation has modified the configuration, storing the changes.");

        storeConfiguration();
      }

      return getConfiguration();
    }
    throw new InvalidConfigurationException(vResponse);
  }

  private void dumpValidationErrors(final ValidationResponse response) {
    if (response.getValidationErrors().size() > 0 || response.getValidationWarnings().size() > 0) {
      log.error("* * * * * * * * * * * * * * * * * * * * * * * * * *");
      log.error("Nexus configuration has validation errors/warnings");
      log.error("* * * * * * * * * * * * * * * * * * * * * * * * * *");

      if (response.getValidationErrors().size() > 0) {
        log.error("The ERRORS:");
        for (ValidationMessage msg : response.getValidationErrors()) {
          log.error(msg.toString());
        }
      }

      if (response.getValidationWarnings().size() > 0) {
        log.error("The WARNINGS:");
        for (ValidationMessage msg : response.getValidationWarnings()) {
          log.error(msg.toString());
        }
      }

      log.error("* * * * * * * * * * * * * * * * * * * * *");
    }
    else {
      log.info("Nexus configuration validated successfully.");
    }
  }

  private void upgradeNexusVersion() throws IOException {
    final String currentVersion = checkNotNull(systemStatusProvider.get().getVersion());
    final String previousVersion = getConfiguration().getNexusVersion();
    if (!currentVersion.equals(previousVersion)) {
      getConfiguration().setNexusVersion(currentVersion);
      storeConfiguration();
    }
  }

  @Override
  public void storeConfiguration() throws IOException {
    saveConfiguration(getConfigurationFile());
  }

  private void loadConfiguration(File file) throws IOException, ConfigurationException {
    loadConfiguration(file.toURI().toURL());

    // seems a bit dirty, but the config might need to be upgraded.
    if (this.getConfiguration() != null) {
      // decrypt the passwords
      setConfiguration(configHelper.encryptDecryptPasswords(getConfiguration(), false));
    }
  }

  private void saveConfiguration(final File file) throws IOException {
    // Create the dir if doesn't exist, throw runtime exception on failure
    // bad bad bad
    try {
      DirSupport.mkdir(file.getParentFile().toPath());
    }
    catch (IOException e) {
      String message =
          "\r\n******************************************************************************\r\n"
              + "* Could not create configuration file [ " + file + "]!!!! *\r\n"
              + "* Nexus cannot start properly until the process has read+write permissions to this folder *\r\n"
              + "******************************************************************************";

      log.error(message, e);
      throw new IOException("Could not create configuration file " + file.getAbsolutePath(), e);
    }

    // Clone the conf so we can encrypt the passwords
    final Configuration configuration = configHelper.encryptDecryptPasswords(getConfiguration(), true);
    log.debug("Saving configuration: {}", file);
    final FileReplacer fileReplacer = new FileReplacer(file);
    // we save this file many times, don't litter backups
    fileReplacer.setDeleteBackupFile(true);
    fileReplacer.replace(new ContentWriter()
    {
      @Override
      public void write(final BufferedOutputStream output)
          throws IOException
      {
        new NexusConfigurationXpp3Writer().write(output, configuration);
      }
    });
  }

  @Override
  public void backupConfiguration() throws IOException {
    File file = getConfigurationFile();

    // backup the file
    File backup = new File(file.getParentFile(), file.getName() + ".bak");
    Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }
}
