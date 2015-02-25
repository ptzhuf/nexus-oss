package org.sonatype.nexus.blobstore.api.configuration;

import org.sonatype.nexus.orient.entity.Entity;

/**
 * @since 3.0
 */
public class BlobStoreConfiguration
  extends Entity
{
  /**
   * The name for the BlobStore.
   */
  private String name;

  /**
   * The path to the root of the BlobStore.
   */
  private String path;
  
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  @Override
  public String toString() {
    return "BlobStoreConfiguration{" +
        "name='" + name + '\'' +
        ", path='" + path + '\'' +
        '}';
  }
}
