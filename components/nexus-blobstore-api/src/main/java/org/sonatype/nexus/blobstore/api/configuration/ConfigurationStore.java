package org.sonatype.nexus.blobstore.api.configuration;

import java.util.List;

public interface ConfigurationStore
{
  List<BlobStoreConfiguration> list();

  void create(BlobStoreConfiguration configuration);

  void update(BlobStoreConfiguration configuration);

  void delete(BlobStoreConfiguration configuration);
}
