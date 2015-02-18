package com.sonatype.nexus.repository.nuget;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class NugetContentUpdatedEvent
    extends NugetContentEvent
{
  public NugetContentUpdatedEvent(final String id, final String version, OrientVertex component) {
    super(id, version, component);
  }
}
