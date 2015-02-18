package com.sonatype.nexus.repository.nuget;

import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class NugetContentDeletedEvent
    extends NugetContentEvent
{
  public NugetContentDeletedEvent(final String id, final String version, OrientVertex component) {
    super(id, version, component);
  }
}
