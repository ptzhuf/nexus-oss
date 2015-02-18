package com.sonatype.nexus.repository.nuget;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public abstract class NugetContentEvent
{
  private final String id;

  private final String version;

  private final OrientVertex component;
  
  public NugetContentEvent(final String id, final String version, final OrientVertex component) {
    this.id = id;
    this.version = version;
    this.component = component;
  }

  public final String getId() {
    return id;
  }

  public final String getVersion() {
    return version;
  }

  public final OrientVertex getComponent() {
    return component;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() +"{" +
        "id='" + id + '\'' +
        ", version='" + version + '\'' +
        ", component=" + component +
        '}';
  }
}
